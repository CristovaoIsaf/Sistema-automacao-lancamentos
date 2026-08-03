package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.service.NotaContaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * T3 — Notas às contas (PGC-AO, secção 5 do Plano Geral de Contabilidade):
 * explica a composição do saldo de uma conta num período, por entidade de
 * origem, com o documento que fundamenta cada movimento.
 */
@RestController
@RequestMapping("/notas")
@RequiredArgsConstructor
public class NotaContaController {

    private final NotaContaService notaContaService;

    @GetMapping("/{conta}")
    public ResponseEntity<NotaContaResponseDTO> obterNota(
            @PathVariable String conta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(notaContaService.obterNota(conta, inicio, fim));
    }
}
