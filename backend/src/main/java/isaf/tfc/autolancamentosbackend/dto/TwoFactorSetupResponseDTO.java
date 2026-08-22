package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resposta de POST /api/2fa/setup — o frontend gera o QR code a partir de
 * `otpauthUrl` (ex.: com a biblioteca `qrcode`); `secret` fica visível ao
 * lado para quem preferir escrever o código manualmente na app
 * autenticadora, em vez de digitalizar o QR.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorSetupResponseDTO {

    private String secret;

    private String otpauthUrl;
}
