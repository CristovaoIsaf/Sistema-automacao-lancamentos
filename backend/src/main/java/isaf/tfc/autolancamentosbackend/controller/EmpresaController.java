package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Âmbito deste TFC: uma única empresa por instalação — por isso não há
 * POST (criar) nem listagem, só consulta/edição do registo já semeado.
 */
@RestController
@RequestMapping("/api/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;

    // Leitura fica aberta a qualquer papel autenticado — nome/NIF da
    // empresa são informação de contexto que Contabilista/Auditor também
    // podem precisar de ver (ex: para conferir o NIF num documento).
    @GetMapping
    public ResponseEntity<EmpresaDTO> obter() {
        return ResponseEntity.ok(empresaService.obterEmpresa());
    }

    // Fase 1 — editar os dados cadastrais da empresa é um acto
    // administrativo. Antes desta fase, qualquer utilizador autenticado
    // conseguia alterar o NIF/nome/morada da empresa.
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @PutMapping
    public ResponseEntity<EmpresaDTO> atualizar(@RequestBody EmpresaDTO dados) {
        return ResponseEntity.ok(empresaService.atualizarEmpresa(dados));
    }
}
