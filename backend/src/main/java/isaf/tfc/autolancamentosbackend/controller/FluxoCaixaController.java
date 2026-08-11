package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.FluxoCaixaResponseDTO;
import isaf.tfc.autolancamentosbackend.service.FluxoCaixaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Fase 17 do plano de 20 fases — Fluxo de Caixa. Mesmo padrão de
 * BalanceteController/DemonstracoesFinanceirasController: inicio/fim
 * opcionais, sem restrição de papel além de autenticação (mesmo nível de
 * acesso dos outros relatórios financeiros).
 */
@RestController
@RequestMapping("/api/fluxo-caixa")
public class FluxoCaixaController {

    private final FluxoCaixaService fluxoCaixaService;

    public FluxoCaixaController(FluxoCaixaService fluxoCaixaService) {
        this.fluxoCaixaService = fluxoCaixaService;
    }

    @GetMapping
    public ResponseEntity<FluxoCaixaResponseDTO> gerar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(fluxoCaixaService.gerarFluxoCaixa(inicio, fim));
    }
}
