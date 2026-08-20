package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.AuditLog;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auditoria C06/C07 — ver AuditLog. REQUIRES_NEW de propósito: um evento de
 * FALHA (ex. login com password errada) tem de sobreviver mesmo que a
 * operação que falhou reverta a sua própria transação — senão a própria
 * prova da tentativa falhada desaparecia com o rollback.
 */
@Service
public class AuditLogService {

    public static final String SUCESSO = "SUCESSO";
    public static final String FALHA = "FALHA";

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registar(User autor, String acao, String entidade, Long entidadeId, String resultado, String motivo, String ip) {
        AuditLog log = new AuditLog();
        log.setUtilizadorId(autor != null ? autor.getId() : null);
        log.setUtilizadorNome(autor != null ? autor.getNome() : null);
        log.setPapel(autor != null && autor.getPapel() != null ? autor.getPapel().name() : null);
        log.setAcao(acao);
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setResultado(resultado);
        log.setMotivo(motivo);
        log.setIp(ip);
        repository.save(log);
    }

    /**
     * Para tentativas de login que falham antes de haver um User resolvido
     * (ex. email inexistente) — grava o email tentado, para não perder o
     * rasto de quem tentou aceder.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registarComEmail(String emailTentado, String acao, String resultado, String motivo, String ip) {
        AuditLog log = new AuditLog();
        log.setUtilizadorNome(emailTentado);
        log.setAcao(acao);
        log.setEntidade("User");
        log.setResultado(resultado);
        log.setMotivo(motivo);
        log.setIp(ip);
        repository.save(log);
    }
}
