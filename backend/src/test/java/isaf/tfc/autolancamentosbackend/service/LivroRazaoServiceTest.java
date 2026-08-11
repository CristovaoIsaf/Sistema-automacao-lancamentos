package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.LivroRazaoResponseDTO;
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
 * Fase 18 do plano de 20 fases — LivroRazaoService isolado.
 * BalanceteService mockado devolvendo Lancamento fabricados (mesmo
 * padrão de FluxoCaixaServiceTest); este teste cobre o filtro por conta,
 * a ordem cronológica e o saldo acumulado.
 */
class LivroRazaoServiceTest {

    private BalanceteService balanceteService;
    private LivroRazaoService service;

    @BeforeEach
    void setUp() {
        balanceteService = Mockito.mock(BalanceteService.class);
        PlanoContasClient planoContasClient = Mockito.mock(PlanoContasClient.class);
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("31", "Clientes", "3", null, "DEVEDORA"),
                new ContaDTO("61", "Vendas", "6", null, "CREDORA")
        ));
        service = new LivroRazaoService(balanceteService, planoContasClient);
    }

    @Test
    void gerarLivroRazao_calculaSaldoAcumuladoEmOrdemCronologica() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(2L, LocalDate.of(2026, 3, 15), "Venda 2",
                        linha("31", new BigDecimal("20000.00"), null), linha("61", null, new BigDecimal("20000.00"))),
                lancamento(1L, LocalDate.of(2026, 3, 5), "Venda 1",
                        linha("31", new BigDecimal("10000.00"), null), linha("61", null, new BigDecimal("10000.00")))
        ));

        LivroRazaoResponseDTO razao = service.gerarLivroRazao("31", null, null);

        assertThat(razao.getConta()).isEqualTo("31");
        assertThat(razao.getNomeConta()).isEqualTo("Clientes");
        assertThat(razao.getMovimentos()).hasSize(2);
        assertThat(razao.getMovimentos().get(0).getLancamentoId()).isEqualTo(1L);
        assertThat(razao.getMovimentos().get(0).getSaldoAcumulado()).isEqualByComparingTo("10000.00");
        assertThat(razao.getMovimentos().get(1).getLancamentoId()).isEqualTo(2L);
        assertThat(razao.getMovimentos().get(1).getSaldoAcumulado()).isEqualByComparingTo("30000.00");
        assertThat(razao.getSaldoFinal()).isEqualByComparingTo("30000.00");
    }

    @Test
    void gerarLivroRazao_ignoraLinhasDeOutrasContas() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(1L, LocalDate.of(2026, 3, 5), "Venda",
                        linha("31", new BigDecimal("10000.00"), null), linha("61", null, new BigDecimal("10000.00")))
        ));

        LivroRazaoResponseDTO razaoVendas = service.gerarLivroRazao("61", null, null);

        assertThat(razaoVendas.getMovimentos()).hasSize(1);
        assertThat(razaoVendas.getMovimentos().get(0).getCredito()).isEqualByComparingTo("10000.00");
        assertThat(razaoVendas.getMovimentos().get(0).getDebito()).isNull();
    }

    @Test
    void gerarLivroRazao_debitosECreditosNaMesmaConta_saldoAcumuladoReflecteSubtracao() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(1L, LocalDate.of(2026, 3, 5), "Venda a crédito",
                        linha("31", new BigDecimal("50000.00"), null)),
                lancamento(2L, LocalDate.of(2026, 3, 20), "Recebimento do cliente",
                        linha("31", null, new BigDecimal("30000.00")))
        ));

        LivroRazaoResponseDTO razao = service.gerarLivroRazao("31", null, null);

        assertThat(razao.getTotalDebito()).isEqualByComparingTo("50000.00");
        assertThat(razao.getTotalCredito()).isEqualByComparingTo("30000.00");
        assertThat(razao.getSaldoFinal()).isEqualByComparingTo("20000.00");
        assertThat(razao.getMovimentos().get(1).getSaldoAcumulado()).isEqualByComparingTo("20000.00");
    }

    @Test
    void gerarLivroRazao_semMovimentos_devolveListaVaziaESaldoZero() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of());

        LivroRazaoResponseDTO razao = service.gerarLivroRazao("31", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(razao.getMovimentos()).isEmpty();
        assertThat(razao.getSaldoFinal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(razao.getTotalDebito()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(razao.getTotalCredito()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void gerarLivroRazao_contaDesconhecidaDoPlanoDeContas_usaOCodigoComoNome() {
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of());

        LivroRazaoResponseDTO razao = service.gerarLivroRazao("99.9.9", null, null);

        assertThat(razao.getNomeConta()).isEqualTo("99.9.9");
    }

    @Test
    void gerarLivroRazao_descricaoDaLinhaTemPrioridadeSobreADoLancamento() {
        LinhaLancamento linhaComDescricaoPropria = linha("31", new BigDecimal("5000.00"), null);
        linhaComDescricaoPropria.setDescricao("IVA dedutível desta linha");
        when(balanceteService.lancamentosValidadosNoIntervalo(any(), any())).thenReturn(List.of(
                lancamento(1L, LocalDate.of(2026, 3, 5), "Descrição do lançamento", linhaComDescricaoPropria)
        ));

        LivroRazaoResponseDTO razao = service.gerarLivroRazao("31", null, null);

        assertThat(razao.getMovimentos().get(0).getDescricao()).isEqualTo("IVA dedutível desta linha");
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
