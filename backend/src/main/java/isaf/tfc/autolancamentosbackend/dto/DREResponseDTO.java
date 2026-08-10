package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demonstração de Resultados por Exercício (Fase 13 do plano de 20
 * fases). Ver DemonstracoesFinanceirasService.gerarDRE — simplificada:
 * este TFC (ver pgc.py) não modela movimento de existências/CMVC, por
 * isso "gastos" trata Compras (21) como gasto do período inteiro, sem
 * distinguir vendido de em stock. Decisão explícita, documentada no
 * serviço, não um esquecimento.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DREResponseDTO {

    private LocalDate inicio;

    private LocalDate fim;

    private List<LinhaDemonstracaoDTO> receitas;

    private BigDecimal totalReceitas;

    private List<LinhaDemonstracaoDTO> gastos;

    private BigDecimal totalGastos;

    private BigDecimal resultadoLiquido;
}
