package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balanço (Fase 13 do plano de 20 fases, Capital Próprio adicionado na
 * Fase 19) — restrito às contas de Terceiros (classe 3), Meios monetários
 * (classe 4) e Capital e Reservas (classe 5, Fase 19), ver
 * DemonstracoesFinanceirasService.gerarBalanco. Este PGC-AO reduzido (ver
 * pgc.py) ainda não modela Ativo Não Corrente — por isso `totalAtivo` e
 * `totalPassivo + totalCapitalProprio` NÃO fecham necessariamente ao mesmo
 * valor (ver `diferenca`).
 *
 * `resultadoExercicio` dentro de `capitalProprio` não é uma conta lançada
 * — é o resultado líquido do MESMO período, obtido da DRE (nunca
 * recalculado aqui, mesma disciplina "fonte única" da Fase 13). Este
 * sistema não tem mecanismo de fecho de exercício (ver BalanceteService),
 * por isso o resultado do período fica sempre em Capital Próprio como
 * "pendente de aplicação" — é assim, e não um erro, que o Balanço se
 * aproxima de fechar a zero sem inventar nenhuma conta de equilíbrio.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalancoResponseDTO {

    private LocalDate inicio;

    private LocalDate fim;

    private List<LinhaDemonstracaoDTO> ativo;

    private BigDecimal totalAtivo;

    private List<LinhaDemonstracaoDTO> passivo;

    private BigDecimal totalPassivo;

    private List<LinhaDemonstracaoDTO> capitalProprio;

    private BigDecimal totalCapitalProprio;

    // totalAtivo - (totalPassivo + totalCapitalProprio) — informativo, não é um erro de cálculo.
    private BigDecimal diferenca;
}
