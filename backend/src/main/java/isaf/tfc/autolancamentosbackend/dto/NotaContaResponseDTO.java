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
public class NotaContaResponseDTO {

    private String conta;

    private LocalDate inicio;

    private LocalDate fim;

    private List<GrupoEntidadeDTO> porEntidade;

    private BigDecimal totalDebito;

    private BigDecimal totalCredito;

    // Diferença aritmética débito-crédito — a leitura como "saldo devedor" ou
    // "saldo credor" depende da natureza da conta consultada (não codificada
    // aqui; ver Decreto 82/01 para a natureza de cada conta).
    private BigDecimal saldo;
}
