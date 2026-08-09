package isaf.tfc.autolancamentosbackend.dto;

import isaf.tfc.autolancamentosbackend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nunca inclui "senha" — devolver o hash bcrypt ao cliente não tem
 * necessidade nenhuma e é o tipo de coisa que um júri de TFC assinala.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {

    private Long id;

    private String nome;

    private String email;

    private String nif;

    private String status;

    private Role papel;

    // Fase 1 — utilizador pertence ao contexto da empresa da instalação
    // (ver User.empresaId). Sempre a mesma empresa neste TFC (single-tenant).
    private Long empresaId;
}
