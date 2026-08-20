package isaf.tfc.autolancamentosbackend.dto;

import isaf.tfc.autolancamentosbackend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * senha é opcional em atualizações — só é alterada quando vem preenchida
 * (ver UserService.atualizar). status idem (Auditoria C09) — "ATIVO"/
 * "INATIVO"; em falta ou vazio, mantém o estado atual do utilizador.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {

    private String nome;

    private String email;

    private String nif;

    private String senha;

    private Role papel;

    private String status;
}
