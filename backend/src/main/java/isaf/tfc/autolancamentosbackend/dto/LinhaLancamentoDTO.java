package isaf.tfc.autolancamentosbackend.dto;

import lombok.*;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinhaLancamentoDTO {


    private String conta;


    private BigDecimal debito;


    private BigDecimal credito;


    private String descricao;

}