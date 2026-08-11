package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.PaginaAuditoriaDTO;
import isaf.tfc.autolancamentosbackend.service.AuditoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fase 15 do plano de 20 fases — "consultar auditoria" (UC010, RF017).
 * Antes desta fase, /auditoria (frontend) mostrava só dados de exemplo
 * estáticos — este endpoint é o primeiro backend real por trás dela (ver
 * AuditoriaService).
 */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    // RN010/UC010 — mesma regra já aplicada em Auditoria.tsx
    // (permissoes().podeVerLogs): Administrador e Auditor, nunca
    // Contabilista.
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'AUDITOR')")
    @GetMapping("/logs")
    public ResponseEntity<PaginaAuditoriaDTO> listarLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limite
    ) {
        return ResponseEntity.ok(auditoriaService.listarLogs(page, limite));
    }
}
