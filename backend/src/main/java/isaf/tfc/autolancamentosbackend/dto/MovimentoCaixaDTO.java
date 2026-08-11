package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovimentoCaixaDTO {

    private Long lancamentoId;

    private LocalDate data;

    private String descricao;

    private String conta;

    private String nomeConta;

    private String contraConta;

    private String tipo;

    private BigDecimal valor;
}
