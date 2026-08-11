package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.BalanceteLinhaDTO;
import isaf.tfc.autolancamentosbackend.dto.BalanceteResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Balancete de verificação: soma débitos/créditos por conta a partir dos
 * Lancamento VALIDADO reais (não dados de exemplo) — o mesmo motor de
 * partidas dobradas já testado em PartidasDobradas/AnaliseContabilService.
 *
 * Simplificação deliberada (sistema ainda sem períodos contabilísticos
 * fechados): não existe "saldo anterior" separado do "movimento do
 * período" — o acumulado É o movimento, porque não há histórico de
 * períodos encerrados antes deste. Filtrar por [inicio, fim] restringe só
 * que lançamentos entram na soma.
 */
@Service
public class BalanceteService {

    private final LancamentoRepository lancamentoRepository;
    private final PlanoContasClient planoContasClient;

    public BalanceteService(LancamentoRepository lancamentoRepository, PlanoContasClient planoContasClient) {
        this.lancamentoRepository = lancamentoRepository;
        this.planoContasClient = planoContasClient;
    }

    /**
     * Fase 17 do plano de 20 fases — extraído de gerarBalancete() para ser
     * reutilizado por FluxoCaixaService, que precisa dos Lancamento (não só
     * do agregado por conta). Mesma fonte única, nunca reimplementada.
     */
    public List<Lancamento> lancamentosValidadosNoIntervalo(LocalDate inicio, LocalDate fim) {
        return lancamentoRepository.findAll().stream()
                .filter(l -> l.getEstado() == EstadoLancamento.VALIDADO)
                .filter(l -> dentroDoIntervalo(l, inicio, fim))
                .toList();
    }

    public BalanceteResponseDTO gerarBalancete(LocalDate inicio, LocalDate fim) {
        Map<String, BigDecimal> debitosPorConta = new LinkedHashMap<>();
        Map<String, BigDecimal> creditosPorConta = new LinkedHashMap<>();

        lancamentosValidadosNoIntervalo(inicio, fim).stream()
                .flatMap(l -> l.getLinhas().stream())
                .forEach(linha -> acumular(linha, debitosPorConta, creditosPorConta));

        List<BalanceteLinhaDTO> linhas = contasEnvolvidas(debitosPorConta, creditosPorConta).stream()
                .map(conta -> construirLinha(conta, debitosPorConta, creditosPorConta))
                .sorted(Comparator.comparing(BalanceteLinhaDTO::getConta))
                .toList();

        BigDecimal totalDebito = somar(linhas, BalanceteLinhaDTO::getDebitoAcumulado);
        BigDecimal totalCredito = somar(linhas, BalanceteLinhaDTO::getCreditoAcumulado);
        BigDecimal totalSaldoDevedor = somar(linhas, BalanceteLinhaDTO::getSaldoDevedor);
        BigDecimal totalSaldoCredor = somar(linhas, BalanceteLinhaDTO::getSaldoCredor);

        boolean equilibrado = totalDebito.compareTo(totalCredito) == 0
                && totalSaldoDevedor.compareTo(totalSaldoCredor) == 0;

        return new BalanceteResponseDTO(linhas, totalDebito, totalCredito, totalSaldoDevedor, totalSaldoCredor, equilibrado);
    }

    private boolean dentroDoIntervalo(Lancamento lancamento, LocalDate inicio, LocalDate fim) {
        if (lancamento.getData() == null) {
            return true;
        }
        if (inicio != null && lancamento.getData().isBefore(inicio)) {
            return false;
        }
        return fim == null || !lancamento.getData().isAfter(fim);
    }

    private void acumular(LinhaLancamento linha, Map<String, BigDecimal> debitos, Map<String, BigDecimal> creditos) {
        String conta = linha.getConta();
        if (conta == null) {
            return;
        }
        if (linha.getDebito() != null) {
            debitos.merge(conta, linha.getDebito(), BigDecimal::add);
        }
        if (linha.getCredito() != null) {
            creditos.merge(conta, linha.getCredito(), BigDecimal::add);
        }
    }

    private List<String> contasEnvolvidas(Map<String, BigDecimal> debitos, Map<String, BigDecimal> creditos) {
        return java.util.stream.Stream.concat(debitos.keySet().stream(), creditos.keySet().stream())
                .distinct()
                .toList();
    }

    private BalanceteLinhaDTO construirLinha(String conta, Map<String, BigDecimal> debitos, Map<String, BigDecimal> creditos) {
        BigDecimal debitoAcumulado = debitos.getOrDefault(conta, BigDecimal.ZERO);
        BigDecimal creditoAcumulado = creditos.getOrDefault(conta, BigDecimal.ZERO);
        BigDecimal saldo = debitoAcumulado.subtract(creditoAcumulado);

        BigDecimal saldoDevedor = saldo.signum() > 0 ? saldo : BigDecimal.ZERO;
        BigDecimal saldoCredor = saldo.signum() < 0 ? saldo.negate() : BigDecimal.ZERO;

        return new BalanceteLinhaDTO(conta, nomeDaConta(conta), debitoAcumulado, creditoAcumulado, saldoDevedor, saldoCredor);
    }

    private String nomeDaConta(String codigo) {
        return planoContasClient.listar().stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .map(ContaDTO::getNome)
                .findFirst()
                .orElse(codigo);
    }

    private BigDecimal somar(List<BalanceteLinhaDTO> linhas, java.util.function.Function<BalanceteLinhaDTO, BigDecimal> campo) {
        return linhas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
