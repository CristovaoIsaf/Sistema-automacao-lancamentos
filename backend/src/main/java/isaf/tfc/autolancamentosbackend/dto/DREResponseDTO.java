package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Demonstração de Resultados por Exercício (Fase 13 do plano de 20
 * fases). Ver DemonstracoesFinanceirasService.gerarDRE — "receitas" é a
 * classe 6 (Proveitos), "gastos" é a classe 7 (Custos). Compras (classe
 * 2 — Existências) NÃO entra aqui: é Ativo, não Custo, por isso aparece
 * no Balanço em vez de "gastos" — este TFC (ver pgc.py) não modela
 * movimento de existências/CMVC, então não há como transformar Compras
 * num "custo das mercadorias vendidas" correto; mostrá-la como saldo no
 * Balanço é a aproximação menos errada das duas.
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
