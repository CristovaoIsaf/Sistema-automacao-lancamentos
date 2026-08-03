package isaf.tfc.autolancamentosbackend.dto;

import lombok.Getter;

@Getter
public class LoginResponseDTO {
    private String token;
    private String tipo = "Bearer";
    private String email;
    private String nome;
    private String papel;

    public LoginResponseDTO(String token, String tipo, String email, String nome, String papel) {
        this.token = token;
        this.tipo = tipo;
        this.email = email;
        this.nome = nome;
        this.papel = papel;
    }
    public LoginResponseDTO(String token) {
        this.token = token;
    }
}
