package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.DashboardAdministradorDTO;
import isaf.tfc.autolancamentosbackend.dto.DashboardResponseDTO;
import isaf.tfc.autolancamentosbackend.service.DashboardAdministradorService;
import isaf.tfc.autolancamentosbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardAdministradorService dashboardAdministradorService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> obterDashboard(
            @RequestParam(defaultValue = "6") int meses,
            @RequestParam(defaultValue = "6") int limite
    ) {
        return ResponseEntity.ok(dashboardService.obterDashboard(meses, limite));
    }

    /**
     * Fase 15 do plano de 20 fases — dados exclusivos da visão de
     * Administrador (atividade por utilizador, pendências). @PreAuthorize
     * aqui, não só escondido no frontend — "NÃO confiar apenas em
     * esconder elementos no frontend".
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @GetMapping("/administrador")
    public ResponseEntity<DashboardAdministradorDTO> obterDashboardAdministrador() {
        return ResponseEntity.ok(dashboardAdministradorService.obter());
    }
}
