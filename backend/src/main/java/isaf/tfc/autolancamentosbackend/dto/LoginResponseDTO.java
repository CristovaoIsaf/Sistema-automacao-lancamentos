package isaf.tfc.autolancamentosbackend.dto;

import lombok.Getter;

@Getter
public class LoginResponseDTO {
    private Long id;
    private String token;
    private String tipo = "Bearer";
    private String email;
    private String nome;
    private String papel;

    // 2FA: quando true, todos os campos acima ficam null/omissos de
    // propósito — nenhum token é emitido no 1º passo do login enquanto o
    // código de dois factores não for confirmado (ver AuthController e
    // TwoFactorAuthService).
    private boolean requiresTwoFactor;

    // Auditoria C01/C03 — o frontend precisa do id numérico do utilizador
    // autenticado para saber, sem esperar por um 403, se pode aprovar um
    // lançamento/anulação que ele próprio criou/pediu (ver
    // Lancamentos.tsx). Antes só vinha token/email/nome/papel — id ficava
    // sempre "-" em AuthContext.tsx.
    public LoginResponseDTO(Long id, String token, String tipo, String email, String nome, String papel) {
        this.id = id;
        this.token = token;
        this.tipo = tipo;
        this.email = email;
        this.nome = nome;
        this.papel = papel;
        this.requiresTwoFactor = false;
    }
    public LoginResponseDTO(String token) {
        this.token = token;
    }

    private LoginResponseDTO(boolean requiresTwoFactor) {
        this.requiresTwoFactor = requiresTwoFactor;
    }

    public static LoginResponseDTO exigeDoisFactores() {
        return new LoginResponseDTO(true);
    }
}
