package isaf.tfc.autolancamentosbackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void runtimeException_mapeiaPara500ComAMensagemMasSemStackTrace() {
        ResponseEntity<Map<String, Object>> resposta =
                handler.tratarRuntimeException(new RuntimeException("Senha inválida"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody()).containsEntry("message", "Senha inválida");
        assertThat(resposta.getBody()).containsEntry("status", 500);
        assertThat(resposta.getBody()).doesNotContainKey("trace");
    }

    @Test
    void responseStatusException_preservaOEstadoEARazao() {
        ResponseEntity<Map<String, Object>> resposta = handler.tratarResponseStatusException(
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas tentativas falhadas."));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getBody()).containsEntry("message", "Demasiadas tentativas falhadas.");
        assertThat(resposta.getBody()).doesNotContainKey("trace");
    }
}
