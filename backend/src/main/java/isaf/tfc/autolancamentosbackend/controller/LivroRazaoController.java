package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.LivroRazaoResponseDTO;
import isaf.tfc.autolancamentosbackend.service.LivroRazaoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Fase 18 do plano de 20 fases — Livro Razão. "conta" vai por query param,
 * não @PathVariable/{conta}: códigos como "34.5.1" têm pontos, que em
 * várias configurações de path matching do Spring MVC são tratados como
 * sufixo de extensão de ficheiro no último segmento — evita esse risco
 * por completo em vez de depender da versão/configuração exata do
 * PathPatternParser.
 */
@RestController
@RequestMapping("/api/livro-razao")
public class LivroRazaoController {

    private final LivroRazaoService livroRazaoService;

    public LivroRazaoController(LivroRazaoService livroRazaoService) {
        this.livroRazaoService = livroRazaoService;
    }

    @GetMapping
    public ResponseEntity<LivroRazaoResponseDTO> gerar(
            @RequestParam String conta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        return ResponseEntity.ok(livroRazaoService.gerarLivroRazao(conta, inicio, fim));
    }
}
