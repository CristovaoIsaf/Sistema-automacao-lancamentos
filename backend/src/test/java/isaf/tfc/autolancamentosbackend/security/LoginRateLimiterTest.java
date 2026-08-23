package isaf.tfc.autolancamentosbackend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demonstrado ao vivo contra o backend real (ver conversa) que
 * /auth/login aceitava tentativas ilimitadas de password E de código 2FA
 * sem qualquer atraso ou bloqueio — este limitador fecha essa lacuna.
 */
class LoginRateLimiterTest {

    private static final class RelogioAjustavel extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }

        void avancar(Duration duracao) { agora = agora.plus(duracao); }
    }

    @Test
    void naoBloqueiaAntesDeQualquerFalha() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        assertFalse(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void naoBloqueiaAbaixoDoLimiteDeTentativas() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        for (int i = 0; i < 4; i++) {
            limiter.registarFalha("utilizador@exemplo.ao");
        }
        assertFalse(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void bloqueiaAoAtingirOLimiteDeTentativas() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        for (int i = 0; i < 5; i++) {
            limiter.registarFalha("utilizador@exemplo.ao");
        }
        assertTrue(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void naoAfetaOutraChave() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        for (int i = 0; i < 5; i++) {
            limiter.registarFalha("vitima@exemplo.ao");
        }
        assertFalse(limiter.estaBloqueado("outro@exemplo.ao"));
    }

    @Test
    void chaveENormalizadaPorEmailIndependenteDeCaixaEEspacos() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        for (int i = 0; i < 5; i++) {
            limiter.registarFalha("  Utilizador@Exemplo.ao  ");
        }
        assertTrue(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void sucessoLimpaOEstadoImediatamente() {
        LoginRateLimiter limiter = new LoginRateLimiter(new RelogioAjustavel());
        for (int i = 0; i < 4; i++) {
            limiter.registarFalha("utilizador@exemplo.ao");
        }
        limiter.registarSucesso("utilizador@exemplo.ao");
        limiter.registarFalha("utilizador@exemplo.ao");
        assertFalse(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void desbloqueiaSozinhoDepoisDaDuracaoDoBloqueio() {
        RelogioAjustavel relogio = new RelogioAjustavel();
        LoginRateLimiter limiter = new LoginRateLimiter(relogio);
        for (int i = 0; i < 5; i++) {
            limiter.registarFalha("utilizador@exemplo.ao");
        }
        assertTrue(limiter.estaBloqueado("utilizador@exemplo.ao"));

        relogio.avancar(Duration.ofMinutes(16));

        assertFalse(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }

    @Test
    void janelaDeContagemExpiraSemAtingirOLimite() {
        RelogioAjustavel relogio = new RelogioAjustavel();
        LoginRateLimiter limiter = new LoginRateLimiter(relogio);
        for (int i = 0; i < 4; i++) {
            limiter.registarFalha("utilizador@exemplo.ao");
        }

        relogio.avancar(Duration.ofMinutes(16));

        // a contagem antiga expirou — precisa de 5 NOVAS falhas, não só mais 1
        limiter.registarFalha("utilizador@exemplo.ao");
        assertFalse(limiter.estaBloqueado("utilizador@exemplo.ao"));
    }
}
