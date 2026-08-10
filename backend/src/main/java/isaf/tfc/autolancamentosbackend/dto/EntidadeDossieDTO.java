package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fase 10 do plano de 20 fases — "criar visão de dossiê da entidade":
 * dados da entidade (cliente/fornecedor) + todos os documentos arquivados
 * sob ela (ver EntidadeController, DocumentoEnriquecimentoService).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntidadeDossieDTO {

    private Long id;

    private String nome;

    private String nif;

    // CLIENTE / FORNECEDOR / DESCONHECIDO
    private String tipo;

    private List<DocumentoResponseDTO> documentos;
}
