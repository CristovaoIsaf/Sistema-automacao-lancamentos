package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;

    // Auditoria/2FA: só preenchido no 2º passo do login, depois do
    // frontend já ter recebido requiresTwoFactor=true na 1ª tentativa (só
    // com email+password). Aceita tanto um código TOTP de 6 dígitos como
    // um código de recuperação (ver TwoFactorAuthService).
    private String codigo2FA;
}
