package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RedacaoNotaDTO;
import isaf.tfc.autolancamentosbackend.service.NotaContaService;
import isaf.tfc.autolancamentosbackend.service.RedacaoNotaClient;
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
    private final RedacaoNotaClient redacaoNotaClient;

    @GetMapping("/{conta}")
    public ResponseEntity<NotaContaResponseDTO> obterNota(
            @PathVariable String conta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(notaContaService.obterNota(conta, inicio, fim));
    }

    /**
     * Fase 14 do plano de 20 fases — rascunho de texto explicativo para a
     * nota (ver RedacaoNotaClient/FastAPI nota_redacao.py). Devolve 204
     * quando o FastAPI não responde — nunca um erro, para a consulta da
     * nota em si (já disponível via GET /notas/{conta}) não depender
     * desta parte opcional.
     */
    @GetMapping("/{conta}/redacao")
    public ResponseEntity<RedacaoNotaDTO> obterRedacao(
            @PathVariable String conta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        NotaContaResponseDTO nota = notaContaService.obterNota(conta, inicio, fim);
        RedacaoNotaDTO redacao = redacaoNotaClient.redigir(nota);
        return redacao != null ? ResponseEntity.ok(redacao) : ResponseEntity.noContent().build();
    }
}
