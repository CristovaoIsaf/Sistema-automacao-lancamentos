package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LivroRazaoResponseDTO {

    private String conta;

    private String nomeConta;

    private LocalDate inicio;

    private LocalDate fim;

    private List<MovimentoRazaoDTO> movimentos;

    private BigDecimal totalDebito;

    private BigDecimal totalCredito;

    private BigDecimal saldoFinal;
}
