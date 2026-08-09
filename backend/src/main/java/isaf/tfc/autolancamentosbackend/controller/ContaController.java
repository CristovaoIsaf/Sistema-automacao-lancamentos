package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.service.PlanoContasClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Plano de Contas — devolve o plano de contas real (Decreto n.º 82/01),
 * a mesma fonte usada pelos lançamentos automáticos.
 *
 * Fase 6 do plano de 20 fases: antes desta fase mantinha aqui uma cópia
 * manual, hardcoded, das contas já definidas em
 * fastapi/app/services/pgc.py — duas listas sincronizadas à mão. Agora só
 * delega em PlanoContasClient, que busca o plano de contas real a
 * GET /pgc/contas (FastAPI); nenhuma conta é definida neste ficheiro.
 */
@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private final PlanoContasClient planoContasClient;

    public ContaController(PlanoContasClient planoContasClient) {
        this.planoContasClient = planoContasClient;
    }

    @GetMapping
    public ResponseEntity<List<ContaDTO>> listar() {
        return ResponseEntity.ok(planoContasClient.listar());
    }
}
