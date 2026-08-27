package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Auditoria de performance: GET /documentos devolvia sempre o arquivo
 * completo (findAll() sem Pageable) — cresce sem limite. Mesma forma já
 * usada por PaginaAuditoriaDTO/PaginaLancamentosDTO (itens + total).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginaDocumentosDTO {

    private List<DocumentoResponseDTO> itens;

    private long total;
}
