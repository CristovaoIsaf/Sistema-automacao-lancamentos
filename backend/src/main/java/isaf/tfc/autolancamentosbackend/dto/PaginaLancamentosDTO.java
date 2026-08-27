package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Auditoria de performance: GET /api/lancamentos devolvia sempre a lista
 * completa (findAll() sem Pageable) — cresce sem limite com o histórico.
 * Mesma forma já usada por PaginaAuditoriaDTO (itens + total), para o
 * frontend não ter dois padrões de paginação diferentes.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginaLancamentosDTO {

    private List<LancamentoResponseDTO> itens;

    private long total;
}
