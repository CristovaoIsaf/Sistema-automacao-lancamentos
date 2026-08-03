package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.DashboardResponseDTO;
import isaf.tfc.autolancamentosbackend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponseDTO> obterDashboard(
            @RequestParam(defaultValue = "6") int meses,
            @RequestParam(defaultValue = "6") int limite
    ) {
        return ResponseEntity.ok(dashboardService.obterDashboard(meses, limite));
    }
}
