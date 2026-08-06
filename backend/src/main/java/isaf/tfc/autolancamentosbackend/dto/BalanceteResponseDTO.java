package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceteResponseDTO {

    private List<BalanceteLinhaDTO> linhas;

    private BigDecimal totalDebito;

    private BigDecimal totalCredito;

    private BigDecimal totalSaldoDevedor;

    private BigDecimal totalSaldoCredor;

    private boolean equilibrado;
}
