package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoHistoricoDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LancamentoHistorico;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.LancamentoHistoricoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LancamentoServiceImpl implements LancamentoService {


    private final LancamentoRepository repository;
    // Fase 9 — único ponto de conversão Lancamento→DTO, partilhado com
    // AnaliseContabilService (ver LancamentoEnriquecimentoService).
    private final LancamentoEnriquecimentoService enriquecimentoService;
    // Auditoria C14 — ver PartidasDobradas.validarContasExistem.
    private final PlanoContasClient planoContasClient;
    // Auditoria C04 — ver atualizar()/listarHistorico() abaixo.
    private final LancamentoHistoricoRepository historicoRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    @Override
    @Transactional
    public LancamentoResponseDTO criarLancamentoManual(
            LancamentoRequestDTO request,
            Long criadoPor
    ) {


        // 1. Criar lançamento
        Lancamento lancamento =
                new Lancamento();


        lancamento.setData(request.getData());

        lancamento.setDescricao(
                request.getDescricao()
        );

        // Auditoria C01: criadoPor só identifica quem lançou — validadoPor
        // fica null até aprovar() ser chamado por um SEGUNDO contabilista
        // (ver aprovar() abaixo). Antes desta correção, esta mesma linha
        // gravava criadoPor em validadoPor e o estado nascia já VALIDADO —
        // a mesma pessoa criava e "aprovava" na mesma operação.
        lancamento.setCriadoPor(criadoPor);

        lancamento.setEstado(
                EstadoLancamento.PENDENTE
        );


        lancamento.setOrigem(
                OrigemLancamento.MANUAL
        );


        // 2. Construir linhas (mesmo domínio partilhado com
        // AnaliseContabilService — ver PartidasDobradas, Fase 7) e validar
        // equilíbrio ANTES de associar ao lançamento a gravar.
        List<LinhaLancamento> linhas =
                PartidasDobradas.construirLinhas(request.getLinhas(), request.getDescricao());

        PartidasDobradas.validarEquilibrio(linhas);
        PartidasDobradas.validarContasExistem(linhas, planoContasClient.listar());

        linhas.forEach(linha -> {
            linha.setLancamento(lancamento);
            lancamento.getLinhas().add(linha);
        });


        // 3. Guardar
        Lancamento salvo =
                repository.save(lancamento);


        return enriquecimentoService.converter(salvo);
    }


    @Override
    @Transactional
    public LancamentoResponseDTO aprovar(Long id, Long aprovadoPor) {

        Lancamento lancamento = buscarEntidade(id);

        if (lancamento.getEstado() != EstadoLancamento.PENDENTE) {
            throw new RuntimeException("Só é possível aprovar um lançamento pendente.");
        }

        // Auditoria C01 — o núcleo da correção: quem criou não pode ser
        // quem aprova, mesmo chamando este endpoint diretamente.
        if (aprovadoPor != null && aprovadoPor.equals(lancamento.getCriadoPor())) {
            throw new RuntimeException("Não pode aprovar um lançamento que criou — precisa de outro contabilista.");
        }

        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setValidadoPor(aprovadoPor);
        // Ver AuditoriaService.eventosDeAlteracaoLancamento — reaproveita o
        // mesmo mecanismo (atualizadoEm/alteradoPor) já usado para
        // editar/cancelar, para a aprovação também ficar visível na
        // auditoria derivada.
        lancamento.setAlteradoPor(aprovadoPor);

        return enriquecimentoService.converter(repository.save(lancamento));
    }


    @Override
    public LancamentoResponseDTO buscarPorId(Long id) {

        return enriquecimentoService.converter(buscarEntidade(id));
    }


    @Override
    public List<LancamentoResponseDTO> listarTodos() {

        return enriquecimentoService.converterTodos(repository.findAll());
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim
    ) {

        return enriquecimentoService.converterTodos(
                repository.findByDataBetween(dataInicio, dataFim)
        );
    }


    @Override
    public List<LancamentoResponseDTO> listarPorPeriodo(
            LocalDate dataInicio,
            LocalDate dataFim,
            EstadoLancamento estado
    ) {

        List<Lancamento> lancamentos = repository
                .findByDataBetween(dataInicio, dataFim)
                .stream()
                .filter(lancamento -> estado == null || lancamento.getEstado() == estado)
                .toList();

        return enriquecimentoService.converterTodos(lancamentos);
    }


    @Override
    @Transactional
    public LancamentoResponseDTO atualizar(
            Long id,
            LancamentoRequestDTO request,
            Long alteradoPor
    ) {

        Lancamento lancamento = buscarEntidade(id);

        if (lancamento.getEstado() == EstadoLancamento.CANCELADO) {
            throw new RuntimeException("Não é possível editar um lançamento cancelado");
        }
        if (lancamento.getEstado() == EstadoLancamento.CANCELAMENTO_PENDENTE) {
            throw new RuntimeException("Não é possível editar um lançamento com pedido de anulação por resolver");
        }

        // Auditoria C04: snapshot de como o lançamento estava ANTES desta
        // edição — sem isto, sobrescrever descricao/data/linhas abaixo
        // perdia os valores anteriores para sempre (só ficava visível o
        // estado final, nunca o "antes"). Guardado mesmo que o pedido não
        // traga linhas novas (também é uma alteração: data/descrição).
        registarHistorico(lancamento, alteradoPor);

        lancamento.setDescricao(request.getDescricao());
        lancamento.setData(request.getData());
        lancamento.setAlteradoPor(alteradoPor);

        // Substitui as linhas de débito/crédito quando o pedido as traz —
        // antes desta alteração este método ignorava-as silenciosamente
        // (o formulário de edição no frontend mostrava as contas como
        // só-leitura por causa disso). O ecrã de edição (LancamentoDiario)
        // envia sempre as linhas completas, nunca um subconjunto parcial.
        if (request.getLinhas() != null && !request.getLinhas().isEmpty()) {
            List<LinhaLancamento> novasLinhas =
                    PartidasDobradas.construirLinhas(request.getLinhas(), request.getDescricao());
            PartidasDobradas.validarEquilibrio(novasLinhas);
            PartidasDobradas.validarContasExistem(novasLinhas, planoContasClient.listar());

            lancamento.getLinhas().clear();
            novasLinhas.forEach(linha -> {
                linha.setLancamento(lancamento);
                lancamento.getLinhas().add(linha);
            });

            // Mesma semântica já usada em aprovarSugestao — só faz sentido
            // para origem AUTOMATICO (ver comentário em Lancamento.editadoManualmente).
            if (lancamento.getOrigem() == OrigemLancamento.AUTOMATICO) {
                lancamento.setEditadoManualmente(true);
            }
        }

        return enriquecimentoService.converter(
                repository.save(lancamento)
        );
    }


    @Override
    @Transactional
    public LancamentoResponseDTO solicitarCancelamento(Long id, String motivo, Long solicitadoPor) {

        if (motivo == null || motivo.isBlank()) {
            throw new RuntimeException("É obrigatório indicar o motivo da anulação.");
        }

        Lancamento lancamento = buscarEntidade(id);

        if (lancamento.getEstado() != EstadoLancamento.VALIDADO) {
            throw new RuntimeException("Só é possível pedir a anulação de um lançamento validado.");
        }

        lancamento.setEstado(EstadoLancamento.CANCELAMENTO_PENDENTE);
        lancamento.setMotivoCancelamento(motivo);
        lancamento.setCancelamentoSolicitadoPor(solicitadoPor);
        lancamento.setAlteradoPor(solicitadoPor);

        return enriquecimentoService.converter(repository.save(lancamento));
    }


    @Override
    @Transactional
    public LancamentoResponseDTO aprovarCancelamento(Long id, Long aprovadoPor) {

        Lancamento lancamento = buscarEntidade(id);

        if (lancamento.getEstado() != EstadoLancamento.CANCELAMENTO_PENDENTE) {
            throw new RuntimeException("Este lançamento não tem nenhum pedido de anulação por resolver.");
        }
        // Auditoria C03 — mesma disciplina de segregação de C01: quem pede
        // a anulação não pode ser quem a aprova.
        if (aprovadoPor != null && aprovadoPor.equals(lancamento.getCancelamentoSolicitadoPor())) {
            throw new RuntimeException("Não pode aprovar a anulação que você próprio pediu — precisa de outro contabilista.");
        }

        lancamento.setEstado(EstadoLancamento.CANCELADO);
        lancamento.setAlteradoPor(aprovadoPor);

        Lancamento estorno = construirEstorno(lancamento, aprovadoPor);

        repository.save(lancamento);
        Lancamento estornoSalvo = repository.save(estorno);

        return enriquecimentoService.converter(estornoSalvo);
    }


    @Override
    @Transactional
    public LancamentoResponseDTO rejeitarCancelamento(Long id, Long rejeitadoPor) {

        Lancamento lancamento = buscarEntidade(id);

        if (lancamento.getEstado() != EstadoLancamento.CANCELAMENTO_PENDENTE) {
            throw new RuntimeException("Este lançamento não tem nenhum pedido de anulação por resolver.");
        }
        if (rejeitadoPor != null && rejeitadoPor.equals(lancamento.getCancelamentoSolicitadoPor())) {
            throw new RuntimeException("Não pode rejeitar a anulação que você próprio pediu — precisa de outro contabilista.");
        }

        // Volta a VALIDADO — motivoCancelamento/cancelamentoSolicitadoPor
        // ficam registados (não apagados) como rasto de que houve um
        // pedido, mesmo que rejeitado.
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setAlteradoPor(rejeitadoPor);

        return enriquecimentoService.converter(repository.save(lancamento));
    }


    /**
     * Auditoria C03 — "estorno/anulação → audit log": em vez de o
     * lançamento original simplesmente desaparecer dos relatórios, cria um
     * novo Lancamento com as mesmas linhas mas débito/crédito trocados
     * (partidas dobradas continuam equilibradas), ligado ao original via
     * estornoDeId — o efeito contabilístico fica sempre rastreável.
     */
    private Lancamento construirEstorno(Lancamento original, Long aprovadoPor) {
        Lancamento estorno = new Lancamento();
        estorno.setData(LocalDate.now());
        estorno.setDescricao("Estorno do lançamento #" + original.getId() + " — " + original.getMotivoCancelamento());
        estorno.setEstado(EstadoLancamento.VALIDADO);
        estorno.setOrigem(OrigemLancamento.MANUAL);
        estorno.setCriadoPor(original.getCancelamentoSolicitadoPor());
        estorno.setValidadoPor(aprovadoPor);
        estorno.setEstornoDeId(original.getId());

        original.getLinhas().forEach(linhaOriginal -> {
            LinhaLancamento linhaInvertida = new LinhaLancamento();
            linhaInvertida.setConta(linhaOriginal.getConta());
            linhaInvertida.setDebito(linhaOriginal.getCredito());
            linhaInvertida.setCredito(linhaOriginal.getDebito());
            linhaInvertida.setDescricao(linhaOriginal.getDescricao());
            linhaInvertida.setLancamento(estorno);
            estorno.getLinhas().add(linhaInvertida);
        });

        return estorno;
    }

    private Lancamento buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lançamento não encontrado"));
    }

    /**
     * Auditoria C04 — grava o estado atual de `lancamento` (antes de
     * qualquer alteração ser aplicada por atualizar()) como uma linha de
     * histórico. Chamado sempre pela mesma transação que edita o
     * lançamento, para nunca gravar um histórico sem a edição
     * correspondente acontecer (ou vice-versa).
     */
    private void registarHistorico(Lancamento lancamento, Long alteradoPor) {
        LancamentoHistorico historico = new LancamentoHistorico();
        historico.setLancamentoId(lancamento.getId());
        historico.setDataAnterior(lancamento.getData());
        historico.setDescricaoAnterior(lancamento.getDescricao());
        historico.setLinhasAnterioresJson(serializarLinhas(lancamento.getLinhas()));
        historico.setAlteradoPor(alteradoPor);
        historicoRepository.save(historico);
    }

    @Override
    public List<LancamentoHistoricoDTO> listarHistorico(Long lancamentoId) {
        List<LancamentoHistorico> historico = historicoRepository.findByLancamentoIdOrderByAlteradoEmDesc(lancamentoId);

        Map<Long, User> usersPorId = userRepository.findAllById(
                historico.stream().map(LancamentoHistorico::getAlteradoPor)
                        .filter(java.util.Objects::nonNull).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        return historico.stream().map(h -> {
            User autor = h.getAlteradoPor() != null ? usersPorId.get(h.getAlteradoPor()) : null;
            return new LancamentoHistoricoDTO(
                    h.getId(),
                    h.getDataAnterior(),
                    h.getDescricaoAnterior(),
                    desserializarLinhas(h.getLinhasAnterioresJson()),
                    h.getAlteradoPor(),
                    autor != null ? autor.getNome() : null,
                    h.getAlteradoEm()
            );
        }).toList();
    }

    private String serializarLinhas(List<LinhaLancamento> linhas) {
        List<LinhaLancamentoDTO> dtos = linhas.stream()
                .map(l -> new LinhaLancamentoDTO(l.getConta(), l.getDebito(), l.getCredito(), l.getDescricao()))
                .toList();
        try {
            return objectMapper.writeValueAsString(dtos);
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível guardar o histórico do lançamento: " + e.getMessage(), e);
        }
    }

    private List<LinhaLancamentoDTO> desserializarLinhas(String linhasJson) {
        if (linhasJson == null || linhasJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    linhasJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LinhaLancamentoDTO.class)
            );
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível ler o histórico do lançamento: " + e.getMessage(), e);
        }
    }
}
