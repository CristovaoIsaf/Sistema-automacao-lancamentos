package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Fase 9 do plano de 20 fases ("histórico ... a criação manual e
 * automática deve utilizar o mesmo mecanismo"): único ponto que converte
 * Lancamento → LancamentoResponseDTO, incluindo os campos usados para
 * filtrar/pesquisar o histórico (documento, entidade, utilizador). Antes
 * desta fase, LancamentoServiceImpl e AnaliseContabilService tinham cada
 * um a sua própria conversão — ambas desactualizadas em relação aos novos
 * campos, uma tê-los-ia e a outra não.
 *
 * Um Lancamento não tem relação directa com Entidade — só alcançável via
 * sugestaoId → Sugestao.documentoId → DocumentoContabilistico.entidadeId
 * (MANUAL não tem sugestaoId, fica sempre sem entidade — correcto, um
 * lançamento manual não tem documento de origem). Os métodos em lote
 * (converterTodos) evitam N+1 queries ao listar o histórico inteiro.
 */
@Service
public class LancamentoEnriquecimentoService {

    private final SugestaoRepository sugestaoRepository;
    private final DocumentoRepository documentoRepository;
    private final EntidadeRepository entidadeRepository;
    private final UserRepository userRepository;

    public LancamentoEnriquecimentoService(SugestaoRepository sugestaoRepository,
                                            DocumentoRepository documentoRepository,
                                            EntidadeRepository entidadeRepository,
                                            UserRepository userRepository) {
        this.sugestaoRepository = sugestaoRepository;
        this.documentoRepository = documentoRepository;
        this.entidadeRepository = entidadeRepository;
        this.userRepository = userRepository;
    }

    public LancamentoResponseDTO converter(Lancamento lancamento) {
        return converterTodos(List.of(lancamento)).get(0);
    }

    public List<LancamentoResponseDTO> converterTodos(List<Lancamento> lancamentos) {
        List<Long> sugestaoIds = idsDistintos(lancamentos, Lancamento::getSugestaoId);
        Map<Long, Sugestao> sugestoesPorId = sugestaoRepository.findAllById(sugestaoIds).stream()
                .collect(Collectors.toMap(Sugestao::getId, s -> s));

        List<Long> documentoIds = sugestoesPorId.values().stream()
                .map(Sugestao::getDocumentoId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, DocumentoContabilistico> documentosPorId = documentoRepository.findAllById(documentoIds).stream()
                .collect(Collectors.toMap(DocumentoContabilistico::getId, d -> d));

        List<Long> entidadeIds = documentosPorId.values().stream()
                .map(DocumentoContabilistico::getEntidadeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Entidade> entidadesPorId = entidadeRepository.findAllById(entidadeIds).stream()
                .collect(Collectors.toMap(Entidade::getId, e -> e));

        // Auditoria C01/C03: além de validadoPor, agora também é preciso
        // resolver o nome de quem criou (criadoPor) e, quando aplicável,
        // de quem pediu a anulação (cancelamentoSolicitadoPor) — mesmo
        // mapa partilhado, para não repetir o findAllById.
        List<Long> todosOsIdsDeUtilizador = java.util.stream.Stream.of(
                        idsDistintos(lancamentos, Lancamento::getValidadoPor),
                        idsDistintos(lancamentos, Lancamento::getCriadoPor),
                        idsDistintos(lancamentos, Lancamento::getCancelamentoSolicitadoPor))
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<Long, User> usersPorId = userRepository.findAllById(todosOsIdsDeUtilizador).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return lancamentos.stream()
                .map(l -> construir(l, sugestoesPorId, documentosPorId, entidadesPorId, usersPorId))
                .toList();
    }

    private List<Long> idsDistintos(List<Lancamento> lancamentos, java.util.function.Function<Lancamento, Long> extrator) {
        return lancamentos.stream().map(extrator).filter(Objects::nonNull).distinct().toList();
    }

    private LancamentoResponseDTO construir(
            Lancamento lancamento,
            Map<Long, Sugestao> sugestoesPorId,
            Map<Long, DocumentoContabilistico> documentosPorId,
            Map<Long, Entidade> entidadesPorId,
            Map<Long, User> usersPorId
    ) {
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

        Sugestao sugestao = lancamento.getSugestaoId() != null ? sugestoesPorId.get(lancamento.getSugestaoId()) : null;
        DocumentoContabilistico documento = sugestao != null && sugestao.getDocumentoId() != null
                ? documentosPorId.get(sugestao.getDocumentoId())
                : null;
        Entidade entidade = documento != null && documento.getEntidadeId() != null
                ? entidadesPorId.get(documento.getEntidadeId())
                : null;

        dto.setDocumentoId(documento != null ? documento.getId() : null);
        dto.setEntidadeId(entidade != null ? entidade.getId() : null);
        dto.setEntidadeNome(entidade != null ? entidade.getNome() : null);

        User validadoPor = lancamento.getValidadoPor() != null ? usersPorId.get(lancamento.getValidadoPor()) : null;
        dto.setValidadoPor(lancamento.getValidadoPor());
        dto.setValidadoPorNome(validadoPor != null ? validadoPor.getNome() : null);

        User criadoPor = lancamento.getCriadoPor() != null ? usersPorId.get(lancamento.getCriadoPor()) : null;
        dto.setCriadoPor(lancamento.getCriadoPor());
        dto.setCriadoPorNome(criadoPor != null ? criadoPor.getNome() : null);

        dto.setMotivoCancelamento(lancamento.getMotivoCancelamento());
        User solicitante = lancamento.getCancelamentoSolicitadoPor() != null
                ? usersPorId.get(lancamento.getCancelamentoSolicitadoPor()) : null;
        dto.setCancelamentoSolicitadoPor(lancamento.getCancelamentoSolicitadoPor());
        dto.setCancelamentoSolicitadoPorNome(solicitante != null ? solicitante.getNome() : null);

        dto.setEstornoDeId(lancamento.getEstornoDeId());

        return dto;
    }
}
