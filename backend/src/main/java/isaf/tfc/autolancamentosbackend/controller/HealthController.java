package isaf.tfc.autolancamentosbackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de healthcheck para a plataforma de hosting (Railway/Render) —
 * sem isto não havia nenhum endpoint público (ver SecurityConfig,
 * .anyRequest().authenticated()) que a plataforma pudesse chamar para
 * confirmar que o processo arrancou, e um healthcheck a bater num
 * endpoint autenticado voltaria sempre 401/403, nunca 200. Mesmo padrão
 * já usado pelo serviço FastAPI (GET /health).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
