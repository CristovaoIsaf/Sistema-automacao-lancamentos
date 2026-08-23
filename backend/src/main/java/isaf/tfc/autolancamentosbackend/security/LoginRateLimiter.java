package isaf.tfc.autolancamentosbackend.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueio temporário por conta em /auth/login — cobre tanto tentativas de
 * password como de código 2FA (ambas passam por AuthController.login()).
 * Antes disto não existia nenhum limite: confirmado ao vivo que 15
 * passwords erradas e 20 códigos 2FA aleatórios seguidos eram todos
 * aceites para verificação sem qualquer atraso, o que tornava um código
 * TOTP de 6 dígitos (1M combinações) atacável por força bruta dentro da
 * janela de validade.
 *
 * Em memória (não partilhado entre instâncias, perdido num reinício) —
 * suficiente para o deployment actual (um único container no Railway);
 * não vale a pena a complexidade de um store partilhado (Redis, etc.)
 * antes de isso ser mesmo necessário.
 */
@Component
public class LoginRateLimiter {

    private static final int LIMITE_TENTATIVAS = 5;
    private static final Duration JANELA_CONTAGEM = Duration.ofMinutes(15);
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(Clock.systemUTC());
    }

    LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean estaBloqueado(String chave) {
        String key = normalizar(chave);
        Estado estado = estados.get(key);
        if (estado == null || estado.bloqueadoAte == null) {
            return false;
        }
        if (clock.instant().isBefore(estado.bloqueadoAte)) {
            return true;
        }
        // Bloqueio expirado — remove para não acumular entradas mortas
        // indefinidamente no mapa.
        estados.remove(key);
        return false;
    }

    public void registarFalha(String chave) {
        String key = normalizar(chave);
        estados.compute(key, (k, estado) -> {
            Instant agora = clock.instant();
            if (estado == null || estado.primeiraFalhaEm == null
                    || agora.isAfter(estado.primeiraFalhaEm.plus(JANELA_CONTAGEM))) {
                estado = new Estado();
                estado.primeiraFalhaEm = agora;
            }
            estado.falhas++;
            if (estado.falhas >= LIMITE_TENTATIVAS) {
                estado.bloqueadoAte = agora.plus(DURACAO_BLOQUEIO);
            }
            return estado;
        });
    }

    public void registarSucesso(String chave) {
        estados.remove(normalizar(chave));
    }

    private String normalizar(String chave) {
        return chave == null ? "" : chave.trim().toLowerCase();
    }

    private static final class Estado {
        int falhas;
        Instant primeiraFalhaEm;
        Instant bloqueadoAte;
    }
}
