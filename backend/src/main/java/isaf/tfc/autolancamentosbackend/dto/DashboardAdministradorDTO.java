package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fase 15 do plano de 20 fases — "o administrador não deve simplesmente
 * receber a mesma interface do contabilista": dados exclusivos da visão
 * de Administrador (ver DashboardAdministradorService), nunca devolvidos
 * pelo endpoint de dashboard partilhado (GET /api/dashboard) — o
 * @PreAuthorize fica no controller, não só escondido no frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardAdministradorDTO {

    private List<AtividadeUtilizadorDTO> atividadePorUtilizador;

    private List<PendenciaDTO> pendencias;
}
