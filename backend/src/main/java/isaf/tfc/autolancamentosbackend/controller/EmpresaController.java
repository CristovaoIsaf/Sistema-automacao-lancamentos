package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public ResponseEntity<EmpresaDTO> obter() {
        return ResponseEntity.ok(empresaService.obterEmpresa());
    }

    @PutMapping
    public ResponseEntity<EmpresaDTO> atualizar(@RequestBody EmpresaDTO dados) {
        return ResponseEntity.ok(empresaService.atualizarEmpresa(dados));
    }
}
