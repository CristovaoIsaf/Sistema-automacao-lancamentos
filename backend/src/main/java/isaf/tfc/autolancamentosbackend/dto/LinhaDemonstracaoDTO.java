package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Uma linha de DRE ou Balanço (Fase 13 do plano de 20 fases) — conta +
 * nome (mesma fonte que BalanceteLinhaDTO, ver PlanoContasClient) + o
 * valor relevante para essa demonstração (crédito acumulado para uma
 * receita, débito acumulado para um gasto, saldo devedor/credor para
 * Ativo/Passivo).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinhaDemonstracaoDTO {

    private String conta;

    private String nome;

    private BigDecimal valor;
}
