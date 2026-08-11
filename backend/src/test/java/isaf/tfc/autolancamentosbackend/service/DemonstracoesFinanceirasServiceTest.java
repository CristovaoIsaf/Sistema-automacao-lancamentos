package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.BalanceteLinhaDTO;
import isaf.tfc.autolancamentosbackend.dto.BalanceteResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.BalancoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.DREResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Fase 13 do plano de 20 fases — "garantir consistência entre lançamentos
 * → balancete → balanço → DRE": DemonstracoesFinanceirasService isolado —
 * BalanceteService mockado devolvendo um BalanceteResponseDTO fabricado
 * (a lógica de acumulação débito/crédito já tem cobertura própria em
 * BalanceteServiceTest; este teste cobre só como DRE/Balanço reagrupam
 * essas linhas por classe/natureza).
 */
class DemonstracoesFinanceirasServiceTest {

    private BalanceteService balanceteService;
    private PlanoContasClient planoContasClient;
    private DemonstracoesFinanceirasService service;

    @BeforeEach
    void setUp() {
        balanceteService = Mockito.mock(BalanceteService.class);
        planoContasClient = Mockito.mock(PlanoContasClient.class);
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("21", "Compras", "2", null, "DEVEDORA"),
                new ContaDTO("61", "Vendas", "6", null, "CREDORA"),
                new ContaDTO("62", "Prestações de serviços", "6", null, "CREDORA"),
                new ContaDTO("75.2.21", "Rendas e alugueres", "7", "75.2.21", "DEVEDORA"),
                new ContaDTO("31", "Clientes", "3", null, "DEVEDORA"),
                new ContaDTO("32", "Fornecedores", "3", null, "CREDORA"),
                new ContaDTO("45", "Caixa", "4", null, "DEVEDORA"),
                new ContaDTO("51", "Capital", "5", null, "CREDORA"),
                new ContaDTO("34.5.1", "IVA dedutível", "3", "34.5.1", "DEVEDORA"),
                new ContaDTO("34.5.2", "IVA liquidado", "3", "34.5.2", "CREDORA")
        ));
        service = new DemonstracoesFinanceirasService(balanceteService, planoContasClient);
    }

    @Test
    void gerarDRE_separaReceitasDeGastosPorClasseESomaResultadoLiquido() {
        BalanceteResponseDTO balancete = new BalanceteResponseDTO(
                List.of(
                        linha("61", "Vendas", BigDecimal.ZERO, new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00")),
                        linha("21", "Compras", new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), BigDecimal.ZERO),
                        linha("75.2.21", "Rendas e alugueres", new BigDecimal("10000.00"), BigDecimal.ZERO, new BigDecimal("10000.00"), BigDecimal.ZERO),
                        // Conta de balanço (classe 4) — nunca deve aparecer na DRE.
                        linha("45", "Caixa", new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"), BigDecimal.ZERO)
                ),
                new BigDecimal("55000.00"), new BigDecimal("100000.00"),
                new BigDecimal("55000.00"), new BigDecimal("100000.00"), false
        );
        when(balanceteService.gerarBalancete(any(), any())).thenReturn(balancete);

        DREResponseDTO dre = service.gerarDRE(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(dre.getReceitas()).hasSize(1);
        assertThat(dre.getReceitas().get(0).getConta()).isEqualTo("61");
        assertThat(dre.getTotalReceitas()).isEqualByComparingTo("100000.00");

        assertThat(dre.getGastos()).extracting(l -> l.getConta()).containsExactlyInAnyOrder("21", "75.2.21");
        assertThat(dre.getTotalGastos()).isEqualByComparingTo("50000.00");

        assertThat(dre.getResultadoLiquido()).isEqualByComparingTo("50000.00");
    }

    @Test
    void gerarBalanco_separaAtivoDePassivoPorNaturezaEExcluiContasDeResultados() {
        BalanceteResponseDTO balancete = new BalanceteResponseDTO(
                List.of(
                        linha("31", "Clientes", new BigDecimal("30000.00"), BigDecimal.ZERO, new BigDecimal("30000.00"), BigDecimal.ZERO),
                        linha("45", "Caixa", new BigDecimal("20000.00"), BigDecimal.ZERO, new BigDecimal("20000.00"), BigDecimal.ZERO),
                        linha("32", "Fornecedores", BigDecimal.ZERO, new BigDecimal("15000.00"), BigDecimal.ZERO, new BigDecimal("15000.00")),
                        linha("34.5.2", "IVA liquidado", BigDecimal.ZERO, new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00")),
                        // Contas de resultados (classe 2/6/7) — já representadas na
                        // DRE, nunca devem ser contadas outra vez aqui.
                        linha("61", "Vendas", BigDecimal.ZERO, new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00")),
                        linha("21", "Compras", new BigDecimal("40000.00"), BigDecimal.ZERO, new BigDecimal("40000.00"), BigDecimal.ZERO)
                ),
                new BigDecimal("90000.00"), new BigDecimal("120000.00"),
                new BigDecimal("50000.00"), new BigDecimal("120000.00"), false
        );
        when(balanceteService.gerarBalancete(any(), any())).thenReturn(balancete);

        BalancoResponseDTO balanco = service.gerarBalanco(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(balanco.getAtivo()).extracting(l -> l.getConta()).containsExactlyInAnyOrder("31", "45");
        assertThat(balanco.getTotalAtivo()).isEqualByComparingTo("50000.00");

        assertThat(balanco.getPassivo()).extracting(l -> l.getConta()).containsExactlyInAnyOrder("32", "34.5.2");
        assertThat(balanco.getTotalPassivo()).isEqualByComparingTo("20000.00");

        // Resultado do período (Fase 19) reaproveitado da DRE: Vendas
        // (100000) - Compras (40000) = 60000, entra em Capital Próprio
        // como "88 — Resultado do Exercício" (nenhuma conta de classe 5
        // lançada neste balancete).
        assertThat(balanco.getCapitalProprio()).extracting(l -> l.getConta()).containsExactly("88");
        assertThat(balanco.getTotalCapitalProprio()).isEqualByComparingTo("60000.00");

        // 50000 (ativo) - (20000 (passivo) + 60000 (capital próprio)) = -30000
        assertThat(balanco.getDiferenca()).isEqualByComparingTo("-30000.00");
    }

    @Test
    void gerarBalanco_incluiContasDeCapitalProprioDaClasse5EResultadoDoExercicio() {
        BalanceteResponseDTO balancete = new BalanceteResponseDTO(
                List.of(
                        linha("45", "Caixa", new BigDecimal("150000.00"), BigDecimal.ZERO, new BigDecimal("150000.00"), BigDecimal.ZERO),
                        linha("51", "Capital", BigDecimal.ZERO, new BigDecimal("100000.00"), BigDecimal.ZERO, new BigDecimal("100000.00")),
                        linha("61", "Vendas", BigDecimal.ZERO, new BigDecimal("60000.00"), BigDecimal.ZERO, new BigDecimal("60000.00")),
                        linha("21", "Compras", new BigDecimal("10000.00"), BigDecimal.ZERO, new BigDecimal("10000.00"), BigDecimal.ZERO)
                ),
                new BigDecimal("160000.00"), new BigDecimal("160000.00"),
                new BigDecimal("150000.00"), new BigDecimal("160000.00"), false
        );
        when(balanceteService.gerarBalancete(any(), any())).thenReturn(balancete);

        BalancoResponseDTO balanco = service.gerarBalanco(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(balanco.getCapitalProprio()).extracting(l -> l.getConta()).containsExactlyInAnyOrder("51", "88");
        var capital = balanco.getCapitalProprio().stream().filter(l -> l.getConta().equals("51")).findFirst().orElseThrow();
        assertThat(capital.getValor()).isEqualByComparingTo("100000.00");
        var resultado = balanco.getCapitalProprio().stream().filter(l -> l.getConta().equals("88")).findFirst().orElseThrow();
        assertThat(resultado.getValor()).isEqualByComparingTo("50000.00");
        assertThat(balanco.getTotalCapitalProprio()).isEqualByComparingTo("150000.00");

        // 150000 (Caixa) - (0 (passivo) + 150000 (capital próprio)) = 0 — fecha a zero.
        assertThat(balanco.getDiferenca()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gerarBalanco_semMovimentoDevolveListasVaziasETotaisZero() {
        BalanceteResponseDTO balanceteVazio = new BalanceteResponseDTO(
                List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, true
        );
        when(balanceteService.gerarBalancete(any(), any())).thenReturn(balanceteVazio);

        BalancoResponseDTO balanco = service.gerarBalanco(null, null);

        assertThat(balanco.getAtivo()).isEmpty();
        assertThat(balanco.getPassivo()).isEmpty();
        assertThat(balanco.getTotalAtivo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balanco.getTotalPassivo()).isEqualByComparingTo(BigDecimal.ZERO);
        // Ainda assim há uma linha sintética "88 — Resultado do Exercício"
        // (0.00) — a DRE de um período sem movimento devolve resultado
        // zero, não uma lista vazia.
        assertThat(balanco.getCapitalProprio()).extracting(l -> l.getConta()).containsExactly("88");
        assertThat(balanco.getTotalCapitalProprio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(balanco.getDiferenca()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private BalanceteLinhaDTO linha(String conta, String nome, BigDecimal debito, BigDecimal credito, BigDecimal saldoDevedor, BigDecimal saldoCredor) {
        return new BalanceteLinhaDTO(conta, nome, debito, credito, saldoDevedor, saldoCredor);
    }
}
