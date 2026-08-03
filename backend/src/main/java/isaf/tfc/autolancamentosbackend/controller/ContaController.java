package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Plano de Contas — espelha literalmente os códigos usados por
 * fastapi/app/services/pgc.py (Decreto n.º 82/01), a fonte oficial de
 * contas realmente usada pelos lançamentos automáticos.
 *
 * Propositadamente NÃO usa a tabela "contas_contabilisticas" da BD nem o
 * mock planoContasAngolano do frontend — esses seguem uma numeração
 * genérica (1=Ativo, 2=Passivo, ...) diferente da classificação real do
 * Decreto 82/01 (1=Disponibilidades, 2=Existências, 3=Terceiros, ...) que o
 * pgc.py implementa. Mostrar essa outra lista aqui criaria uma página de
 * "Plano de Contas" com contas diferentes das que os lançamentos realmente
 * usam.
 */
@RestController
@RequestMapping("/api/contas")
public class ContaController {

    private static final List<ContaDTO> CONTAS = List.of(
            new ContaDTO("21", "Compras"),
            new ContaDTO("26", "Mercadorias"),
            new ContaDTO("31", "Clientes"),
            new ContaDTO("32", "Fornecedores"),
            new ContaDTO("43", "Depósitos à ordem"),
            new ContaDTO("45", "Caixa"),
            new ContaDTO("61", "Vendas"),
            new ContaDTO("62", "Prestações de serviços"),
            new ContaDTO("75", "Outros custos e perdas operacionais"),
            new ContaDTO("75.2.11", "Água"),
            new ContaDTO("75.2.12", "Electricidade"),
            new ContaDTO("75.2.14", "Conservação e reparação"),
            new ContaDTO("75.2.20", "Comunicação"),
            new ContaDTO("75.2.21", "Rendas e alugueres"),
            new ContaDTO("75.2.22", "Seguros"),
            new ContaDTO("75.2.23", "Deslocações e estadas"),
            // Extensão do projeto (autorizada pelo autor do TFC): o Decreto 82/01 é
            // anterior ao IVA em Angola (2019) e não define conta oficial para este imposto.
            new ContaDTO("34.5.1", "IVA dedutível"),
            new ContaDTO("34.5.2", "IVA liquidado")
    );

    @GetMapping
    public ResponseEntity<List<ContaDTO>> listar() {
        return ResponseEntity.ok(CONTAS);
    }
}
