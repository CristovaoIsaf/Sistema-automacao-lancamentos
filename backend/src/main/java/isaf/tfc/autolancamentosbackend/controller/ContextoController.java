package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.ContextoClassificacaoDTO;
import isaf.tfc.autolancamentosbackend.service.ContextoClassificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Fase 3 do plano de 20 fases — expõe o Context Engine
 * (ContextoClassificacaoService) directamente, para o resultado ser
 * inspecionável/testável independentemente de uma análise em curso
 * (cumpre o requisito "reutilizável" — não é só uma função interna
 * chamada uma vez em AnaliseContabilService).
 */
@RestController
@RequestMapping("/api/contexto")
@RequiredArgsConstructor
public class ContextoController {

    private final ContextoClassificacaoService contextoClassificacaoService;

    @GetMapping("/entidade/{entidadeId}")
    public ResponseEntity<ContextoClassificacaoDTO> paraEntidade(@PathVariable Long entidadeId) {
        return ResponseEntity.ok(contextoClassificacaoService.construirContexto(entidadeId));
    }

    // Contexto só com dados da empresa, sem entidade — útil para
    // documentos ainda não ligados a nenhuma entidade conhecida.
    @GetMapping
    public ResponseEntity<ContextoClassificacaoDTO> semEntidade() {
        return ResponseEntity.ok(contextoClassificacaoService.construirContexto(null));
    }
}
