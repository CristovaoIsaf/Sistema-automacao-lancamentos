package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovimentoContaDTO {

    private Long lancamentoId;

    private LocalDate data;

    private String descricao;

    // null quando o lançamento é manual, sem documento de origem associado.
    private Long documentoId;

    private String documentoNome;

    private BigDecimal debito;

    private BigDecimal credito;
}
