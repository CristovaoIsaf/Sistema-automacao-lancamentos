package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

/**
 * Corpo de POST /api/lancamentos/{id}/solicitar-cancelamento — Auditoria
 * C03: motivo passou a ser obrigatório (ver
 * LancamentoServiceImpl.solicitarCancelamento).
 */
@Data
public class SolicitarCancelamentoRequestDTO {

    private String motivo;
}
