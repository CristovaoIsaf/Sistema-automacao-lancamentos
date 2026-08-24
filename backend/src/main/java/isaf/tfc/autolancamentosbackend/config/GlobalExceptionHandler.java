package isaf.tfc.autolancamentosbackend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sem isto, uma exceção lançada num controller (ex.: AuthController,
 * TwoFactorController — sempre RuntimeException, nunca havia um handler
 * próprio) propagava até ao tratamento de erro automático do Spring Boot,
 * que devolve o stack trace completo no corpo JSON quando corre com
 * DevTools activo (mvn spring-boot:run) — visível a qualquer cliente,
 * incluindo não autenticado, em /auth/login. Isto também fazia com que o
 * MockMvc (TwoFactorControllerTest) recebesse a excepção em bruto em vez
 * de uma resposta HTTP normal, porque sem handler nenhum resolver a
 * intercetava antes do container.
 *
 * Os códigos de estado mantêm-se exactamente os mesmos de antes (500 para
 * RuntimeException — nunca foi feita nenhuma tentativa de mapear cada
 * mensagem para um código mais específico, para não mudar comportamento
 * já assumido por testes existentes) — só deixa de incluir o stack trace
 * no corpo. AccessDeniedException (lançada pelos @PreAuthorize em
 * AnaliseController/DocumentoController/etc.) precisa de handler próprio
 * — é também uma RuntimeException, por isso sem isto o handler genérico
 * abaixo apanhava-a primeiro e trocava um 403 legítimo por um 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> tratarResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return corpoErro(status, ex.getReason());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> tratarAccessDeniedException(AccessDeniedException ex) {
        return corpoErro(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> tratarRuntimeException(RuntimeException ex) {
        return corpoErro(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> corpoErro(HttpStatus status, String mensagem) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", status.getReasonPhrase());
        corpo.put("message", mensagem);
        return ResponseEntity.status(status).body(corpo);
    }
}
