package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta de GET /api/2fa/status — sempre sobre o utilizador autenticado,
 * nunca sobre outro (ver TwoFactorController).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorStatusResponseDTO {

    private boolean ativo;
}
