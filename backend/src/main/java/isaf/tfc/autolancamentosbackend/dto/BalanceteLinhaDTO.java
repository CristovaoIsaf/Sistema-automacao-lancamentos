package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceteLinhaDTO {

    private String conta;

    private String nome;

    private BigDecimal debitoAcumulado;

    private BigDecimal creditoAcumulado;

    private BigDecimal saldoDevedor;

    private BigDecimal saldoCredor;
}
