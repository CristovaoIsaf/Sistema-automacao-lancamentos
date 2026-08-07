package isaf.tfc.autolancamentosbackend.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import isaf.tfc.autolancamentosbackend.dto.AprovarSugestaoRequest;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaSugeridaDTO;
import isaf.tfc.autolancamentosbackend.model.*;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnaliseContabilService {

    private final AnalisadorDocumentoIA analisadorDocumentoIA;
    private final DocumentoRepository documentoRepository;
    private final SugestaoRepository sugestaoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final EntidadeService entidadeService;
    private final ObjectMapper objectMapper;

    public AnaliseContabilService(AnalisadorDocumentoIA analisadorDocumentoIA,
                                   DocumentoRepository documentoRepository,
                                   SugestaoRepository sugestaoRepository,
                                   LancamentoRepository lancamentoRepository,
                                   EntidadeService entidadeService,
                                   ObjectMapper objectMapper) {
        this.analisadorDocumentoIA = analisadorDocumentoIA;
        this.documentoRepository = documentoRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.entidadeService = entidadeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Passo 1: busca os bytes do documento já carregado, envia-os à API de análise
     * (OCR + regex + Gemini/regras + PGC-AO) e grava o resultado como uma Sugestao
     * com estado PENDENTE. Ainda não é um lançamento oficial.
     */
    public Sugestao analisarDocumento(Long documentoId) {
        DocumentoContabilistico documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RuntimeException("Documento não encontrado: " + documentoId));

        AnaliseResponse analise = analisadorDocumentoIA.analisar(
                documento.getConteudo(), documento.getNomeFicheiro());

        if (analise == null) {
            throw new RuntimeException("A API de análise devolveu uma resposta vazia.");
        }

        Sugestao sugestao = new Sugestao();
        sugestao.setDocumentoId(documentoId);
        sugestao.setTipoDocumento(valorOuDefeito(analise.getTipoDocumento(), "a_classificar"));
        sugestao.setCategoriaContabil(extrairCategoriaContabil(analise.getLinhas()));
        sugestao.setValor(valorOuDefeito(analise.getValorTotal(), "0"));
        sugestao.setEntidade(analise.getEntidade());
        sugestao.setNif(analise.getNif());
        sugestao.setDescricao(valorOuDefeito(analise.getDescricao(), "Análise não disponível"));
        sugestao.setTextoOriginal(analise.getTextoOcr());
        sugestao.setLinhasJson(serializarLinhas(analise.getLinhas()));
        sugestao.setFundamentacao(analise.getFundamentacao());
        sugestao.setCategoria(analise.getCategoria());
        sugestao.setEstado(EstadoSugestao.PENDENTE);

        Sugestao salva = sugestaoRepository.save(sugestao);

        // T2 — arquivo por entidade: resolve/cria a entidade de origem (cliente
        // ou fornecedor) a partir do NIF/nome que a análise devolveu, e liga-a
        // ao documento para a exportação em ZIP conseguir agrupar por pasta.
        Entidade entidade = entidadeService.resolverOuCriar(
                analise.getNif(), analise.getEntidade(), sugestao.getTipoDocumento());
        documento.setEntidadeId(entidade.getId());
        documentoRepository.save(documento);

        return salva;
    }

    /**
     * Passo 2: aprova uma Sugestao existente, criando o Lancamento oficial
     * (origem AUTOMATICO) com as LinhaLancamento correspondentes.
     *
     * Marco 4: usa todas as linhas equilibradas (débito+crédito) já calculadas
     * pelo pgc_ao e guardadas em linhasJson — não só uma linha a débito. Só
     * recorre a uma única linha a débito se a Sugestao não tiver linhasJson
     * (compatibilidade com sugestões antigas, criadas antes desta alteração).
     */
    public LancamentoResponseDTO aprovarSugestao(Long sugestaoId, Long validadoPor) {
        return aprovarSugestao(sugestaoId, validadoPor, null);
    }

    /**
     * Variante usada pelo fluxo de revisão em LancamentoDiario.tsx: o
     * contabilista pode ter alterado as linhas mostradas no ecrã antes de
     * aprovar — nesse caso `request.linhas` chega preenchido e é usado tal
     * como está, em vez de reler linhasJson. `request` pode ser null/vazio
     * (compatibilidade com chamadas antigas, ex. testes existentes).
     */
    public LancamentoResponseDTO aprovarSugestao(Long sugestaoId, Long validadoPor, AprovarSugestaoRequest request) {
        Sugestao sugestao = sugestaoRepository.findById(sugestaoId)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada: " + sugestaoId));

        if (sugestao.getEstado() == EstadoSugestao.APROVADA) {
            throw new RuntimeException("Sugestão já foi aprovada anteriormente.");
        }
        if (sugestao.getEstado() == EstadoSugestao.REJEITADA) {
            throw new RuntimeException("Sugestão foi anulada e já não pode ser aprovada.");
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setSugestaoId(sugestao.getId());
        lancamento.setValidadoPor(validadoPor);
        lancamento.setData(LocalDate.now());
        lancamento.setDescricao(sugestao.getDescricao());
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);

        boolean temOverride = request != null && request.getLinhas() != null && !request.getLinhas().isEmpty();

        if (temOverride) {
            for (LinhaLancamentoDTO linhaDTO : request.getLinhas()) {
                LinhaLancamento linha = new LinhaLancamento();
                linha.setConta(linhaDTO.getConta());
                linha.setDebito(linhaDTO.getDebito());
                linha.setCredito(linhaDTO.getCredito());
                linha.setDescricao(linhaDTO.getDescricao() != null ? linhaDTO.getDescricao() : sugestao.getDescricao());
                linha.setLancamento(lancamento);
                lancamento.getLinhas().add(linha);
            }
            PartidasDobradas.validarEquilibrio(lancamento.getLinhas());
            lancamento.setEditadoManualmente(request.isEditado());
        } else {
            List<LinhaSugeridaDTO> linhasSugeridas = desserializarLinhas(sugestao.getLinhasJson());

            if (linhasSugeridas.isEmpty()) {
                LinhaLancamento linha = new LinhaLancamento();
                linha.setConta(sugestao.getCategoriaContabil());
                linha.setDebito(PartidasDobradas.parseValor(sugestao.getValor()));
                linha.setCredito(null);
                linha.setDescricao(sugestao.getDescricao());
                linha.setLancamento(lancamento);
                lancamento.getLinhas().add(linha);
            } else {
                for (LinhaSugeridaDTO linhaSugerida : linhasSugeridas) {
                    LinhaLancamento linha = new LinhaLancamento();
                    linha.setConta(linhaSugerida.getConta());
                    linha.setDebito(PartidasDobradas.parseValorNullable(linhaSugerida.getDebito()));
                    linha.setCredito(PartidasDobradas.parseValorNullable(linhaSugerida.getCredito()));
                    linha.setDescricao(sugestao.getDescricao());
                    linha.setLancamento(lancamento);
                    lancamento.getLinhas().add(linha);
                }
                PartidasDobradas.validarEquilibrio(lancamento.getLinhas());
            }
        }

        Lancamento salvo = lancamentoRepository.save(lancamento);

        sugestao.setEstado(EstadoSugestao.APROVADA);
        sugestao.setLancamentoId(salvo.getId());
        sugestaoRepository.save(sugestao);

        return converterParaDTO(salvo);
    }

    /**
     * Anula uma Sugestao pendente — o contabilista decidiu que não deve
     * originar nenhum Lancamento. Não mexe em nada além do estado.
     */
    public void rejeitarSugestao(Long sugestaoId) {
        Sugestao sugestao = sugestaoRepository.findById(sugestaoId)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada: " + sugestaoId));

        if (sugestao.getEstado() != EstadoSugestao.PENDENTE) {
            throw new RuntimeException("Só é possível anular uma sugestão ainda pendente.");
        }

        sugestao.setEstado(EstadoSugestao.REJEITADA);
        sugestaoRepository.save(sugestao);
    }

    private String valorOuDefeito(String valor, String defeito) {
        return (valor == null || valor.isBlank()) ? defeito : valor;
    }

    /**
     * A API de análise devolve várias linhas de partidas dobradas sugeridas;
     * a primeira é a conta a débito, usada como classificação principal da
     * Sugestao (e, mais tarde, como conta da LinhaLancamento única gerada em
     * aprovarSugestao — ver a nota sobre o Marco 4 acima).
     */
    private String extrairCategoriaContabil(List<LinhaSugeridaDTO> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            return null;
        }
        return linhas.get(0).getConta();
    }

    /**
     * Converte a entidade Lancamento para DTO antes de a devolver ao
     * controller — devolver a entidade diretamente serializa também a
     * relação inversa LinhaLancamento.lancamento, criando um ciclo.
     */
    private LancamentoResponseDTO converterParaDTO(Lancamento lancamento) {
        LancamentoResponseDTO dto = new LancamentoResponseDTO();

        dto.setId(lancamento.getId());
        dto.setData(lancamento.getData());
        dto.setDescricao(lancamento.getDescricao());
        dto.setEstado(lancamento.getEstado());
        dto.setOrigem(lancamento.getOrigem());
        dto.setEditadoManualmente(lancamento.getEditadoManualmente());

        List<LinhaLancamentoDTO> linhas = lancamento.getLinhas().stream()
                .map(linha -> new LinhaLancamentoDTO(
                        linha.getConta(),
                        linha.getDebito(),
                        linha.getCredito(),
                        linha.getDescricao()))
                .toList();

        dto.setLinhas(linhas);

        return dto;
    }

    private String serializarLinhas(List<LinhaSugeridaDTO> linhas) {
        if (linhas == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(linhas);
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível guardar as linhas sugeridas: " + e.getMessage(), e);
        }
    }

    private List<LinhaSugeridaDTO> desserializarLinhas(String linhasJson) {
        if (linhasJson == null || linhasJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    linhasJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LinhaSugeridaDTO.class)
            );
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível ler as linhas sugeridas: " + e.getMessage(), e);
        }
    }
}
