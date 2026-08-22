package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

/**
 * Corpo de POST /api/2fa/confirmar — o código de 6 dígitos gerado pela app
 * autenticadora, para confirmar que o setup (POST /api/2fa/setup) foi feito
 * corretamente antes de ativar a exigência de dois factores no login.
 */
@Data
public class TwoFactorCodigoRequestDTO {

    private String codigo;
}
