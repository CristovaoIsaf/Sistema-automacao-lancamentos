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
public class FluxoCaixaResponseDTO {

    private LocalDate inicio;

    private LocalDate fim;

    private List<MovimentoCaixaDTO> movimentos;

    private BigDecimal totalEntradas;

    private BigDecimal totalSaidas;

    private BigDecimal saldoPeriodo;
}
