package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.BalanceteResponseDTO;
import isaf.tfc.autolancamentosbackend.service.BalanceteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/balancetes")
public class BalanceteController {

    private final BalanceteService balanceteService;

    public BalanceteController(BalanceteService balanceteService) {
        this.balanceteService = balanceteService;
    }

    @GetMapping
    public ResponseEntity<BalanceteResponseDTO> gerar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(balanceteService.gerarBalancete(inicio, fim));
    }
}
