package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.BalanceteResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Peça "Balancete": BalanceteService isolado (repositório mockado) — soma
 * débitos/créditos reais por conta a partir de Lancamento VALIDADO, ligando
 * ao motor de partidas dobradas já testado (ver PartidasDobradasTest).
 */
class BalanceteServiceTest {

    private LancamentoRepository lancamentoRepository;
    private BalanceteService service;

    @BeforeEach
    void setUp() {
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        PlanoContasClient planoContasClient = Mockito.mock(PlanoContasClient.class);
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("31", "Clientes"),
                new ContaDTO("61", "Vendas"),
                new ContaDTO("34.5.2", "IVA liquidado")
        ));
        service = new BalanceteService(lancamentoRepository, planoContasClient);
    }

    @Test
    void gerarBalancete_lancamentoEquilibrado_produzSaldosCorretosEEquilibrados() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now(),
                        linha("31", new BigDecimal("50000.00"), null),
                        linha("61", null, new BigDecimal("43859.65")),
                        linha("34.5.2", null, new BigDecimal("6140.35"))
                )
        ));

        BalanceteResponseDTO resposta = service.gerarBalancete(null, null);

        assertThat(resposta.getLinhas()).hasSize(3);
        assertThat(resposta.isEquilibrado()).isTrue();

        var clientes = resposta.getLinhas().stream().filter(l -> l.getConta().equals("31")).findFirst().orElseThrow();
        assertThat(clientes.getSaldoDevedor()).isEqualByComparingTo("50000.00");
        assertThat(clientes.getSaldoCredor()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(clientes.getNome()).isEqualTo("Clientes");
    }

    @Test
    void gerarBalancete_ignoraLancamentosNaoValidados() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinhas(EstadoLancamento.PENDENTE, LocalDate.now(),
                        linha("31", new BigDecimal("100000.00"), null))
        ));

        BalanceteResponseDTO resposta = service.gerarBalancete(null, null);

        assertThat(resposta.getLinhas()).isEmpty();
    }

    @Test
    void gerarBalancete_somaVariosLancamentosNaMesmaConta() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now(),
                        linha("31", new BigDecimal("10000.00"), null), linha("61", null, new BigDecimal("10000.00"))),
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now(),
                        linha("31", new BigDecimal("5000.00"), null), linha("61", null, new BigDecimal("5000.00")))
        ));

        BalanceteResponseDTO resposta = service.gerarBalancete(null, null);

        var clientes = resposta.getLinhas().stream().filter(l -> l.getConta().equals("31")).findFirst().orElseThrow();
        assertThat(clientes.getDebitoAcumulado()).isEqualByComparingTo("15000.00");
    }

    @Test
    void gerarBalancete_filtraPorIntervaloDeDatas() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now().minusMonths(2),
                        linha("31", new BigDecimal("10000.00"), null)),
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now(),
                        linha("31", new BigDecimal("5000.00"), null))
        ));

        BalanceteResponseDTO resposta = service.gerarBalancete(LocalDate.now().minusDays(1), LocalDate.now());

        var clientes = resposta.getLinhas().stream().filter(l -> l.getConta().equals("31")).findFirst().orElseThrow();
        assertThat(clientes.getDebitoAcumulado()).isEqualByComparingTo("5000.00");
    }

    @Test
    void gerarBalancete_semLancamentos_devolveListaVaziaEEquilibrado() {
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        BalanceteResponseDTO resposta = service.gerarBalancete(null, null);

        assertThat(resposta.getLinhas()).isEmpty();
        assertThat(resposta.isEquilibrado()).isTrue();
    }

    @Test
    void gerarBalancete_contaDesconhecidaDoPlanoDeContas_usaOCodigoComoNome() {
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinhas(EstadoLancamento.VALIDADO, LocalDate.now(),
                        linha("99.9.9", new BigDecimal("1000.00"), null))
        ));

        BalanceteResponseDTO resposta = service.gerarBalancete(null, null);

        assertThat(resposta.getLinhas().get(0).getNome()).isEqualTo("99.9.9");
    }

    private Lancamento lancamentoComLinhas(EstadoLancamento estado, LocalDate data, LinhaLancamento... linhas) {
        Lancamento lancamento = new Lancamento();
        lancamento.setEstado(estado);
        lancamento.setData(data);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);
        lancamento.setDescricao("Teste balancete");
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
