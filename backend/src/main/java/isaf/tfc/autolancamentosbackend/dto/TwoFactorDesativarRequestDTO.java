package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

/**
 * Corpo de POST /api/2fa/desativar — exige a password atual (não basta
 * estar autenticado) para desligar os dois factores; sem isto, um token
 * roubado seria suficiente para um atacante desativar a proteção extra que
 * os dois factores davam precisamente contra esse cenário.
 */
@Data
public class TwoFactorDesativarRequestDTO {

    private String password;
}
