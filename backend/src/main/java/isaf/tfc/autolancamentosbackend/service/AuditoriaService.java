package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AtividadeUtilizadorDTO;
import isaf.tfc.autolancamentosbackend.dto.LogAuditoriaDTO;
import isaf.tfc.autolancamentosbackend.dto.PaginaAuditoriaDTO;
import isaf.tfc.autolancamentosbackend.model.AuditLog;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.AuditLogRepository;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fase 15 do plano de 20 fases — "consultar auditoria" / "acompanhar
 * contabilistas": regista de auditoria DERIVADO dos eventos que o sistema
 * já regista de forma fiável — não introduz uma tabela de log nova nem
 * intercepta cada operação de escrita (isso exigiria alterar dezenas de
 * pontos de entrada, fora do âmbito desta fase). Cobre os dois eventos
 * mais relevantes para auditoria já timestampados no sistema:
 *
 *   - Upload de documento (DocumentoContabilistico.userId + dataUpload)
 *   - Criação/aprovação de lançamento (Lancamento.validadoPor + criadoEm)
 *
 * Antes desta fase, /auditoria (frontend) mostrava só dados de exemplo
 * estáticos (`logsMock`), sem nenhum backend real. Ficam FORA do âmbito
 * (não incluídos aqui, para não fingir cobertura que não existe): criação
 * de utilizadores, alterações à empresa/configurações, rejeição de
 * sugestões — nenhum destes tem timestamp próprio persistido hoje.
 *
 * Fase 16 do plano de 20 fases — "auditor: alterações": acrescenta edição
 * e cancelamento de lançamentos (Lancamento.atualizadoEm + alteradoPor),
 * a alteração mais visível que faltava. Limitação assumida: atualizadoEm/
 * alteradoPor guardam só a ÚLTIMA alteração — um histórico completo de
 * edições de um lançamento fica em LancamentoHistorico (Auditoria C04,
 * ver GET /api/lancamentos/{id}/historico), não nesta vista geral.
 *
 * Auditoria C06/C07: as lacunas "criação de utilizadores, alterações à
 * empresa/configurações, rejeição de sugestões" apontadas acima ficaram
 * cobertas pela tabela AuditLog (ver eventosDeAuditLog) — apensa, com
 * resultado (sucesso/falha), motivo e IP, ao contrário dos eventos
 * derivados desta classe.
 */
@Service
public class AuditoriaService {

    private final DocumentoRepository documentoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AuditoriaService(DocumentoRepository documentoRepository,
                             LancamentoRepository lancamentoRepository,
                             UserRepository userRepository,
                             AuditLogRepository auditLogRepository) {
        this.documentoRepository = documentoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public PaginaAuditoriaDTO listarLogs(Integer page, Integer limite) {
        List<LogAuditoriaDTO> eventos = listarTodosOsEventos();

        int tamanhoPagina = (limite != null && limite > 0) ? limite : 20;
        int paginaAtual = (page != null && page > 0) ? page : 0;
        int inicio = Math.min(paginaAtual * tamanhoPagina, eventos.size());
        int fim = Math.min(inicio + tamanhoPagina, eventos.size());

        return new PaginaAuditoriaDTO(eventos.subList(inicio, fim), eventos.size());
    }

    /**
     * Fase 15 — "acompanhar contabilistas": mesmos eventos, agrupados por
     * utilizador, para o Administrador ver quem tem feito o quê sem
     * percorrer a lista inteira.
     */
    public List<AtividadeUtilizadorDTO> resumoPorUtilizador() {
        Map<String, List<LogAuditoriaDTO>> porUtilizador = listarTodosOsEventos().stream()
                .collect(Collectors.groupingBy(LogAuditoriaDTO::getUtilizador));

        return porUtilizador.entrySet().stream()
                .map(entrada -> new AtividadeUtilizadorDTO(
                        entrada.getKey(),
                        entrada.getValue().isEmpty() ? null : entrada.getValue().get(0).getPerfil(),
                        entrada.getValue().size(),
                        entrada.getValue().stream()
                                .map(LogAuditoriaDTO::getDataHora)
                                .filter(Objects::nonNull)
                                .max(Comparator.naturalOrder())
                                .orElse(null)
                ))
                .sorted(Comparator.comparing(AtividadeUtilizadorDTO::getTotalAcoes).reversed())
                .toList();
    }

    private List<LogAuditoriaDTO> listarTodosOsEventos() {
        Map<Long, User> usersPorId = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<LogAuditoriaDTO> eventos = new ArrayList<>();
        eventos.addAll(eventosDeUpload(usersPorId));
        eventos.addAll(eventosDeLancamento(usersPorId));
        eventos.addAll(eventosDeAlteracaoLancamento(usersPorId));
        eventos.addAll(eventosDeAuditLog());

        eventos.sort(Comparator.comparing(
                LogAuditoriaDTO::getDataHora,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return eventos;
    }

    private List<LogAuditoriaDTO> eventosDeUpload(Map<Long, User> usersPorId) {
        return documentoRepository.findAll().stream()
                .filter(d -> d.getDataUpload() != null)
                .map(d -> converterUpload(d, usersPorId.get(d.getUserId())))
                .toList();
    }

    private LogAuditoriaDTO converterUpload(DocumentoContabilistico documento, User utilizador) {
        return new LogAuditoriaDTO(
                "doc-" + documento.getId(),
                utilizador != null ? utilizador.getNome() : "Utilizador desconhecido",
                utilizador != null && utilizador.getPapel() != null ? utilizador.getPapel().name() : null,
                "Importou documento",
                documento.getNomeFicheiro(),
                documento.getDataUpload(),
                AuditLogService.SUCESSO,
                null,
                null
        );
    }

    private List<LogAuditoriaDTO> eventosDeLancamento(Map<Long, User> usersPorId) {
        return lancamentoRepository.findAll().stream()
                .map(l -> converterLancamento(l, autorDaCriacao(l, usersPorId)))
                .toList();
    }

    // Auditoria C01: um lançamento MANUAL já não é validado na criação —
    // validadoPor fica null até aprovar() ser chamado (ver
    // LancamentoServiceImpl) — por isso o EVENTO DE CRIAÇÃO passou a
    // atribuir-se a criadoPor, não a validadoPor. AUTOMATICO mantém-se em
    // validadoPor: aprovar uma Sugestao da IA continua a ser o próprio acto
    // de criação do Lancamento.
    private User autorDaCriacao(Lancamento lancamento, Map<Long, User> usersPorId) {
        Long autorId = lancamento.getOrigem() == OrigemLancamento.MANUAL
                ? lancamento.getCriadoPor()
                : lancamento.getValidadoPor();
        return autorId != null ? usersPorId.get(autorId) : null;
    }

    private LogAuditoriaDTO converterLancamento(Lancamento lancamento, User utilizador) {
        String acao = lancamento.getOrigem() == OrigemLancamento.MANUAL
                ? "Registou lançamento manual"
                : "Aprovou sugestão da IA";

        // criadoEm (Fase 15, ver Lancamento.aoCriar) é o timestamp real de
        // criação — lançamentos registados ANTES desta fase não o têm;
        // nesse caso cai-se para a data contabilística (menos precisa,
        // mas melhor que excluir o evento da auditoria).
        LocalDateTime dataHora = lancamento.getCriadoEm() != null
                ? lancamento.getCriadoEm()
                : (lancamento.getData() != null ? lancamento.getData().atStartOfDay() : null);

        return new LogAuditoriaDTO(
                "lan-" + lancamento.getId(),
                utilizador != null ? utilizador.getNome() : "Utilizador desconhecido",
                utilizador != null && utilizador.getPapel() != null ? utilizador.getPapel().name() : null,
                acao,
                lancamento.getDescricao(),
                dataHora,
                AuditLogService.SUCESSO,
                null,
                null
        );
    }

    // Fase 16 — "alterações": só lançamentos com atualizadoEm preenchido
    // tiveram alguma edição/cancelamento depois de criados (ver
    // Lancamento.aoAtualizar). Lançamentos nunca alterados não geram
    // evento nenhum aqui — o evento de criação já cobre esses.
    private List<LogAuditoriaDTO> eventosDeAlteracaoLancamento(Map<Long, User> usersPorId) {
        return lancamentoRepository.findAll().stream()
                .filter(l -> l.getAtualizadoEm() != null)
                .map(l -> converterAlteracaoLancamento(l, l.getAlteradoPor() != null ? usersPorId.get(l.getAlteradoPor()) : null))
                .toList();
    }

    private LogAuditoriaDTO converterAlteracaoLancamento(Lancamento lancamento, User utilizador) {
        String acao = lancamento.getEstado() == EstadoLancamento.CANCELADO
                ? "Cancelou lançamento"
                : "Editou lançamento";

        return new LogAuditoriaDTO(
                "lan-alt-" + lancamento.getId(),
                utilizador != null ? utilizador.getNome() : "Utilizador desconhecido",
                utilizador != null && utilizador.getPapel() != null ? utilizador.getPapel().name() : null,
                acao,
                lancamento.getDescricao(),
                lancamento.getAtualizadoEm(),
                AuditLogService.SUCESSO,
                null,
                null
        );
    }

    // Auditoria C06/C07 — eventos vindos da tabela apensa AuditLog (login,
    // gestão de utilizadores, alteração da empresa, rejeição de sugestões).
    // Ao contrário dos eventos derivados acima, trazem resultado/motivo/ip
    // reais, porque foram gravados no momento exato da operação — nunca
    // reconstruídos a partir de outra tabela.
    private List<LogAuditoriaDTO> eventosDeAuditLog() {
        return auditLogRepository.findAll().stream()
                .map(this::converterAuditLog)
                .toList();
    }

    private LogAuditoriaDTO converterAuditLog(AuditLog log) {
        LogAuditoriaDTO dto = new LogAuditoriaDTO(
                "audit-" + log.getId(),
                log.getUtilizadorNome() != null ? log.getUtilizadorNome() : "Utilizador desconhecido",
                log.getPapel(),
                log.getAcao(),
                log.getEntidade() != null ? log.getEntidade() + (log.getEntidadeId() != null ? " #" + log.getEntidadeId() : "") : null,
                log.getTimestamp(),
                log.getResultado(),
                log.getMotivo(),
                log.getIp()
        );
        return dto;
    }
}
