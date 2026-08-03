package isaf.tfc.autolancamentosbackend.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaSugeridaDTO;
import isaf.tfc.autolancamentosbackend.model.*;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        sugestao.setDescricao(valorOuDefeito(analise.getDescricao(), "Análise não disponível"));
        sugestao.setTextoOriginal(analise.getTextoOcr());
        sugestao.setLinhasJson(serializarLinhas(analise.getLinhas()));
        sugestao.setFundamentacao(analise.getFundamentacao());
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
        Sugestao sugestao = sugestaoRepository.findById(sugestaoId)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada: " + sugestaoId));

        if (sugestao.getEstado() == EstadoSugestao.APROVADA) {
            throw new RuntimeException("Sugestão já foi aprovada anteriormente.");
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setSugestaoId(sugestao.getId());
        lancamento.setValidadoPor(validadoPor);
        lancamento.setData(LocalDate.now());
        lancamento.setDescricao(sugestao.getDescricao());
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);

        List<LinhaSugeridaDTO> linhasSugeridas = desserializarLinhas(sugestao.getLinhasJson());

        if (linhasSugeridas.isEmpty()) {
            LinhaLancamento linha = new LinhaLancamento();
            linha.setConta(sugestao.getCategoriaContabil());
            linha.setDebito(parseValor(sugestao.getValor()));
            linha.setCredito(null);
            linha.setDescricao(sugestao.getDescricao());
            linha.setLancamento(lancamento);
            lancamento.getLinhas().add(linha);
        } else {
            for (LinhaSugeridaDTO linhaSugerida : linhasSugeridas) {
                LinhaLancamento linha = new LinhaLancamento();
                linha.setConta(linhaSugerida.getConta());
                linha.setDebito(parseValorNullable(linhaSugerida.getDebito()));
                linha.setCredito(parseValorNullable(linhaSugerida.getCredito()));
                linha.setDescricao(sugestao.getDescricao());
                linha.setLancamento(lancamento);
                lancamento.getLinhas().add(linha);
            }
            validarEquilibrio(lancamento.getLinhas());
        }

        Lancamento salvo = lancamentoRepository.save(lancamento);

        sugestao.setEstado(EstadoSugestao.APROVADA);
        sugestao.setLancamentoId(salvo.getId());
        sugestaoRepository.save(sugestao);

        return converterParaDTO(salvo);
    }

    /**
     * Converte o valor em texto devolvido pela API de análise para BigDecimal.
     *
     * A API de análise já devolve os valores normalizados com ponto decimal
     * (ex: "150000.00" — ver pgc.py::_dec). Só trata "." como separador de
     * milhares quando também há vírgula no texto (formato PT-AO "50.000,00"),
     * para não estragar valores que já vêm corretos.
     */
    private BigDecimal parseValor(String valorTexto) {
        if (valorTexto == null || valorTexto.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpo = valorTexto.replaceAll("[^0-9,.-]", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Como parseValor, mas devolve null (em vez de ZERO) quando o texto está
     * vazio — necessário para preservar null no lado da linha que não é usado
     * (débito OU crédito), tal como o pgc_ao já faz.
     */
    private BigDecimal parseValorNullable(String valorTexto) {
        if (valorTexto == null || valorTexto.isBlank()) {
            return null;
        }
        return parseValor(valorTexto);
    }

    /**
     * Verificação defensiva: as linhas devolvidas pela API de análise já
     * deviam estar equilibradas (pgc_ao valida isso do lado do FastAPI), mas
     * confirma-se aqui também antes de gravar o Lancamento oficial.
     */
    private void validarEquilibrio(List<LinhaLancamento> linhas) {
        BigDecimal totalDebito = linhas.stream()
                .map(l -> l.getDebito() != null ? l.getDebito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredito = linhas.stream()
                .map(l -> l.getCredito() != null ? l.getCredito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebito.compareTo(totalCredito) != 0) {
            throw new RuntimeException("O lançamento automático não está equilibrado.");
        }
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
