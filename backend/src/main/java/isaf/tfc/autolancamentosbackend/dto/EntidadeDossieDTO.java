package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fase 10 do plano de 20 fases — "criar visão de dossiê da entidade":
 * dados da entidade (cliente/fornecedor) + todos os documentos arquivados
 * sob ela (ver EntidadeController, DocumentoEnriquecimentoService).
 *
 * `perfil` (Fase 12 — "Perfil de Entidade"): conhecimento acumulado sobre
 * o comportamento habitual desta entidade (ver PerfilEntidadeClient,
 * FastAPI entity_profile.py) — pode ser null quando o FastAPI está
 * indisponível ou a entidade ainda não tem histórico suficiente.
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

    private PerfilEntidadeDTO perfil;
}
