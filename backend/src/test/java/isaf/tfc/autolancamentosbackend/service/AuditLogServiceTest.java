package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.AuditLog;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Peça "AuditLogService" do desenvolvimento incremental — testa isoladamente
 * (repositório mockado com Mockito, sem Spring context/BD real) a montagem
 * do AuditLog a partir de User/email tentado.
 */
class AuditLogServiceTest {

    private AuditLogRepository repository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AuditLogRepository.class);
        service = new AuditLogService(repository);
    }

    private User utilizador() {
        User user = new User();
        user.setId(7L);
        user.setNome("Ana Contabilista");
        user.setPapel(Role.CONTABILISTA);
        return user;
    }

    @Test
    void registar_comUtilizador_preencheTodosOsCamposAPartirDoUser() {
        service.registar(utilizador(), "LOGIN", "User", 7L, AuditLogService.SUCESSO, "motivo qualquer", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUtilizadorId()).isEqualTo(7L);
        assertThat(log.getUtilizadorNome()).isEqualTo("Ana Contabilista");
        assertThat(log.getPapel()).isEqualTo("CONTABILISTA");
        assertThat(log.getAcao()).isEqualTo("LOGIN");
        assertThat(log.getEntidade()).isEqualTo("User");
        assertThat(log.getEntidadeId()).isEqualTo(7L);
        assertThat(log.getResultado()).isEqualTo(AuditLogService.SUCESSO);
        assertThat(log.getMotivo()).isEqualTo("motivo qualquer");
        assertThat(log.getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    void registar_semUtilizador_naoRebentaEDeixaCamposDeUtilizadorNulos() {
        // Ex.: tentativa de login com password errada mas email já resolvido
        // para um User -- ver registarComEmail() para o caso "email nem
        // sequer existe".
        service.registar(null, "LOGIN", "User", null, AuditLogService.FALHA, "Password inválida", "10.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUtilizadorId()).isNull();
        assertThat(log.getUtilizadorNome()).isNull();
        assertThat(log.getPapel()).isNull();
        assertThat(log.getResultado()).isEqualTo(AuditLogService.FALHA);
    }

    @Test
    void registar_utilizadorSemPapel_naoRebentaEDeixaPapelNulo() {
        User semPapel = utilizador();
        semPapel.setPapel(null);

        service.registar(semPapel, "LOGIN", "User", 7L, AuditLogService.SUCESSO, null, "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPapel()).isNull();
    }

    @Test
    void registarComEmail_gravaOEmailTentadoComoUtilizadorNomeEEntidadeUser() {
        service.registarComEmail("desconhecido@exemplo.ao", "LOGIN", AuditLogService.FALHA, "Utilizador não encontrado", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();

        assertThat(log.getUtilizadorId()).isNull();
        assertThat(log.getUtilizadorNome()).isEqualTo("desconhecido@exemplo.ao");
        assertThat(log.getEntidade()).isEqualTo("User");
        assertThat(log.getResultado()).isEqualTo(AuditLogService.FALHA);
        assertThat(log.getMotivo()).isEqualTo("Utilizador não encontrado");
    }
}
