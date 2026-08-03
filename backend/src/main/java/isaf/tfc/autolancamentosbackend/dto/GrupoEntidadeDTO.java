package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrupoEntidadeDTO {

    private String entidade;

    // CLIENTE / FORNECEDOR / DESCONHECIDO / null (lançamento manual, sem documento de origem)
    private String tipo;

    private List<MovimentoContaDTO> movimentos;

    private BigDecimal subtotalDebito;

    private BigDecimal subtotalCredito;
}
