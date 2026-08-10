package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balanço (Fase 13 do plano de 20 fases) — restrito às contas de Terceiros
 * (classe 3) e Meios monetários (classe 4), ver
 * DemonstracoesFinanceirasService.gerarBalanco. Este PGC-AO reduzido (ver
 * pgc.py) não modela Ativo Não Corrente nem Capital Próprio/Património
 * Líquido — por isso `totalAtivo` e `totalPassivo` NÃO fecham
 * necessariamente ao mesmo valor (ver `diferenca`); nunca inventar aqui
 * uma conta de Capital Próprio só para "equilibrar" visualmente.
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

    // totalAtivo - totalPassivo — informativo, não é um erro de cálculo.
    private BigDecimal diferenca;
}
