package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovimentoRazaoDTO {

    private Long lancamentoId;

    private LocalDate data;

    private String descricao;

    private BigDecimal debito;

    private BigDecimal credito;

    private BigDecimal saldoAcumulado;
}
