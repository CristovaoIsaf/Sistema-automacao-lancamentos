package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.FluxoCaixaResponseDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
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
 * Fase 17 do plano de 20 fases — FluxoCaixaService isolado.
 * BalanceteService mockado devolvendo Lancamento fabricados
 * (lancamentosValidadosNoIntervalo já tem cobertura própria em
 * BalanceteServiceTest); este teste cobre só como os movimentos das
 * contas de classe 4 (Caixa/Depósitos) viram entradas/saídas.
 */
class FluxoCaixaServiceTest {

    private BalanceteService balanceteService;
    private FluxoCaixaService service;

    @BeforeEach
    void setUp() {
        balanceteService = Mockito.mock(BalanceteService.class);
        PlanoContasClient planoContasClient = Mockito.mock(PlanoContasClient.class);
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("31", "Clientes", "3", null, "DEVEDORA"),
                new ContaDTO("32", "Fornecedores", "3", null, "CREDORA"),
                new ContaDTO("61", "Vendas", "6", null, "CREDORA"),
                new ContaDTO("45", "Caixa", "4", null, "DEVEDORA"),
                new ContaDTO("46", "Depósitos à ordem", "4", null, "DEVEDORA")
        ));
        service = new FluxoCaixaService(balanceteService, planoContasClient);
    }

    @Test
    void gerarFluxoCaixa_debitoNaContaDeCaixa_geraMovimentoDeEntradaComContraConta() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(1L, LocalDate.of(2026, 3, 10), "Recebimento de cliente",
                        linha("45", new BigDecimal("50000.00"), null),
                        linha("31", null, new BigDecimal("50000.00")))
        ));

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(null, null);

        assertThat(fluxo.getMovimentos()).hasSize(1);
        var movimento = fluxo.getMovimentos().get(0);
        assertThat(movimento.getTipo()).isEqualTo("ENTRADA");
        assertThat(movimento.getConta()).isEqualTo("45");
        assertThat(movimento.getNomeConta()).isEqualTo("Caixa");
        assertThat(movimento.getContraConta()).isEqualTo("Clientes");
        assertThat(movimento.getValor()).isEqualByComparingTo("50000.00");
        assertThat(fluxo.getTotalEntradas()).isEqualByComparingTo("50000.00");
        assertThat(fluxo.getTotalSaidas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo("50000.00");
    }

    @Test
    void gerarFluxoCaixa_creditoNaContaDeCaixa_geraMovimentoDeSaida() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(2L, LocalDate.of(2026, 3, 12), "Pagamento a fornecedor",
                        linha("32", new BigDecimal("20000.00"), null),
                        linha("45", null, new BigDecimal("20000.00")))
        ));

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(null, null);

        assertThat(fluxo.getMovimentos()).hasSize(1);
        var movimento = fluxo.getMovimentos().get(0);
        assertThat(movimento.getTipo()).isEqualTo("SAIDA");
        assertThat(movimento.getContraConta()).isEqualTo("Fornecedores");
        assertThat(fluxo.getTotalSaidas()).isEqualByComparingTo("20000.00");
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo("-20000.00");
    }

    @Test
    void gerarFluxoCaixa_lancamentoSemNenhumaContaDeCaixa_naoGeraMovimento() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(3L, LocalDate.of(2026, 3, 15), "Venda a crédito",
                        linha("31", new BigDecimal("30000.00"), null),
                        linha("61", null, new BigDecimal("30000.00")))
        ));

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(null, null);

        assertThat(fluxo.getMovimentos()).isEmpty();
        assertThat(fluxo.getTotalEntradas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fluxo.getTotalSaidas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gerarFluxoCaixa_somaVariosMovimentosEOrdenaPorData() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(5L, LocalDate.of(2026, 3, 20), "Pagamento renda",
                        linha("32", new BigDecimal("15000.00"), null),
                        linha("45", null, new BigDecimal("15000.00"))),
                lancamento(4L, LocalDate.of(2026, 3, 5), "Recebimento cliente",
                        linha("45", new BigDecimal("40000.00"), null),
                        linha("31", null, new BigDecimal("40000.00")))
        ));

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(null, null);

        assertThat(fluxo.getMovimentos()).extracting(m -> m.getLancamentoId()).containsExactly(4L, 5L);
        assertThat(fluxo.getTotalEntradas()).isEqualByComparingTo("40000.00");
        assertThat(fluxo.getTotalSaidas()).isEqualByComparingTo("15000.00");
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo("25000.00");
    }

    @Test
    void gerarFluxoCaixa_transferenciaEntreContasDeCaixa_contraContaFicaNula() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(6L, LocalDate.of(2026, 3, 22), "Depósito de numerário",
                        linha("46", new BigDecimal("10000.00"), null),
                        linha("45", null, new BigDecimal("10000.00")))
        ));

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(null, null);

        assertThat(fluxo.getMovimentos()).hasSize(2);
        assertThat(fluxo.getMovimentos()).allMatch(m -> m.getContraConta() == null);
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gerarFluxoCaixa_semLancamentos_devolveListaVaziaETotaisZero() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of());

        FluxoCaixaResponseDTO fluxo = service.gerarFluxoCaixa(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(fluxo.getMovimentos()).isEmpty();
        assertThat(fluxo.getTotalEntradas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fluxo.getTotalSaidas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fluxo.getSaldoPeriodo()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Lancamento lancamento(Long id, LocalDate data, String descricao, LinhaLancamento... linhas) {
        Lancamento lancamento = new Lancamento();
        lancamento.setId(id);
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setData(data);
        lancamento.setDescricao(descricao);
        lancamento.setOrigem(OrigemLancamento.MANUAL);
        for (LinhaLancamento linha : linhas) {
            lancamento.getLinhas().add(linha);
        }
        return lancamento;
    }

    private LinhaLancamento linha(String conta, BigDecimal debito, BigDecimal credito) {
        LinhaLancamento linha = new LinhaLancamento();
        linha.setConta(conta);
        linha.setDebito(debito);
        linha.setCredito(credito);
        return linha;
    }
}
