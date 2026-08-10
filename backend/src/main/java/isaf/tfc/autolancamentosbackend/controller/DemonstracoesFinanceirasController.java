package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.BalancoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.DREResponseDTO;
import isaf.tfc.autolancamentosbackend.service.DemonstracoesFinanceirasService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Fase 13 do plano de 20 fases — Demonstrações Financeiras (DRE, Balanço).
 * Mesmo padrão de BalanceteController: inicio/fim opcionais (sem filtro =
 * todos os Lancamento VALIDADO).
 */
@RestController
@RequestMapping("/api/demonstracoes")
public class DemonstracoesFinanceirasController {

    private final DemonstracoesFinanceirasService demonstracoesFinanceirasService;

    public DemonstracoesFinanceirasController(DemonstracoesFinanceirasService demonstracoesFinanceirasService) {
        this.demonstracoesFinanceirasService = demonstracoesFinanceirasService;
    }

    @GetMapping("/dre")
    public ResponseEntity<DREResponseDTO> dre(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(demonstracoesFinanceirasService.gerarDRE(inicio, fim));
    }

    @GetMapping("/balanco")
    public ResponseEntity<BalancoResponseDTO> balanco(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(demonstracoesFinanceirasService.gerarBalanco(inicio, fim));
    }
}
