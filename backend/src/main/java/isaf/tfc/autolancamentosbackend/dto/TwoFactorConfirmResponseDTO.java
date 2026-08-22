package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resposta de POST /api/2fa/confirmar — os códigos de recuperação em texto
 * simples, devolvidos UMA ÚNICA VEZ (só o hash BCrypt fica guardado — ver
 * User.twoFactorRecoveryCodesJson). O frontend tem de os mostrar de forma
 * óbvia para o utilizador os guardar nesse momento; não há nenhum endpoint
 * para os voltar a consultar depois.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorConfirmResponseDTO {

    private List<String> codigosRecuperacao;
}
