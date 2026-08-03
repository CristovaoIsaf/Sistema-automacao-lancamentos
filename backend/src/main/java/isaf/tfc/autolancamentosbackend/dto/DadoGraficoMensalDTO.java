package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DadoGraficoMensalDTO {

    private String mes;

    private BigDecimal receitas;

    private BigDecimal despesas;
}
