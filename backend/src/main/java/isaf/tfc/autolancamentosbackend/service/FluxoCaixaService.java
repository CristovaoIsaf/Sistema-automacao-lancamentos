package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.FluxoCaixaResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.MovimentoCaixaDTO;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fase 17 do plano de 20 fases — Fluxo de Caixa pelo método direto,
 * limitado às contas de Meios Monetários (classe 4 — Caixa/Depósitos).
 *
 * Decisão explícita (a mesma já discutida e descartada na Fase 13, ver
 * DemonstracoesFinanceirasService): NÃO classifica os movimentos em
 * atividades operacionais/investimento/financiamento — esse agrupamento
 * clássico do método indireto exigiria uma regra de classificação por
 * tipo de operação que este plano de contas (pgc.py, "âmbito compra,
 * venda, pagamentos") não modela, e inventar uma seria o mesmo erro já
 * evitado na Fase 4 ("inventar um código de conta não verificado seria
 * pior do que deixar por classificar"). Em vez disso, lista os
 * movimentos reais de caixa (método direto), com a contraconta de cada
 * lançamento para dar contexto de origem/destino do dinheiro.
 *
 * Reutiliza BalanceteService.lancamentosValidadosNoIntervalo() — nunca
 * volta a interrogar o LancamentoRepository diretamente, mesma
 * disciplina de fonte única já aplicada nas Fases 6/7/13.
 */
@Service
public class FluxoCaixaService {

    private static final String CLASSE_MEIOS_MONETARIOS = "4";
    private static final String ENTRADA = "ENTRADA";
    private static final String SAIDA = "SAIDA";

    private final BalanceteService balanceteService;
    private final PlanoContasClient planoContasClient;

    public FluxoCaixaService(BalanceteService balanceteService, PlanoContasClient planoContasClient) {
        this.balanceteService = balanceteService;
        this.planoContasClient = planoContasClient;
    }

    public FluxoCaixaResponseDTO gerarFluxoCaixa(LocalDate inicio, LocalDate fim) {
        Map<String, ContaDTO> contasPorCodigo = contasPorCodigo();
        Set<String> contasCaixa = contasDaClasse(contasPorCodigo, CLASSE_MEIOS_MONETARIOS);

        List<MovimentoCaixaDTO> movimentos = balanceteService.lancamentosValidadosNoIntervalo(inicio, fim).stream()
                .flatMap(lancamento -> movimentosDoLancamento(lancamento, contasCaixa, contasPorCodigo).stream())
                .sorted(Comparator.comparing(MovimentoCaixaDTO::getData, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        BigDecimal totalEntradas = somar(movimentos, ENTRADA);
        BigDecimal totalSaidas = somar(movimentos, SAIDA);

        return new FluxoCaixaResponseDTO(inicio, fim, movimentos, totalEntradas, totalSaidas, totalEntradas.subtract(totalSaidas));
    }

    private List<MovimentoCaixaDTO> movimentosDoLancamento(Lancamento lancamento, Set<String> contasCaixa, Map<String, ContaDTO> contasPorCodigo) {
        List<LinhaLancamento> linhasCaixa = lancamento.getLinhas().stream()
                .filter(l -> l.getConta() != null && contasCaixa.contains(l.getConta()))
                .toList();
        if (linhasCaixa.isEmpty()) {
            return List.of();
        }

        String contraConta = contraConta(lancamento, contasCaixa, contasPorCodigo);

        return linhasCaixa.stream()
                .flatMap(linha -> movimentosDaLinha(lancamento, linha, contraConta, contasPorCodigo).stream())
                .toList();
    }

    private List<MovimentoCaixaDTO> movimentosDaLinha(Lancamento lancamento, LinhaLancamento linha, String contraConta, Map<String, ContaDTO> contasPorCodigo) {
        List<MovimentoCaixaDTO> resultado = new ArrayList<>();
        String nomeConta = nomeDaConta(linha.getConta(), contasPorCodigo);

        if (linha.getDebito() != null && linha.getDebito().signum() > 0) {
            resultado.add(new MovimentoCaixaDTO(lancamento.getId(), lancamento.getData(), lancamento.getDescricao(),
                    linha.getConta(), nomeConta, contraConta, ENTRADA, linha.getDebito()));
        }
        if (linha.getCredito() != null && linha.getCredito().signum() > 0) {
            resultado.add(new MovimentoCaixaDTO(lancamento.getId(), lancamento.getData(), lancamento.getDescricao(),
                    linha.getConta(), nomeConta, contraConta, SAIDA, linha.getCredito()));
        }
        return resultado;
    }

    private String contraConta(Lancamento lancamento, Set<String> contasCaixa, Map<String, ContaDTO> contasPorCodigo) {
        Set<String> nomes = lancamento.getLinhas().stream()
                .filter(l -> l.getConta() != null && !contasCaixa.contains(l.getConta()))
                .map(l -> nomeDaConta(l.getConta(), contasPorCodigo))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return nomes.isEmpty() ? null : String.join(", ", nomes);
    }

    private Map<String, ContaDTO> contasPorCodigo() {
        return planoContasClient.listar().stream()
                .collect(Collectors.toMap(ContaDTO::getCodigo, Function.identity()));
    }

    private Set<String> contasDaClasse(Map<String, ContaDTO> contasPorCodigo, String classe) {
        return contasPorCodigo.values().stream()
                .filter(c -> classe.equals(c.getClasse()))
                .map(ContaDTO::getCodigo)
                .collect(Collectors.toSet());
    }

    private String nomeDaConta(String codigo, Map<String, ContaDTO> contasPorCodigo) {
        ContaDTO conta = contasPorCodigo.get(codigo);
        return conta != null ? conta.getNome() : codigo;
    }

    private BigDecimal somar(List<MovimentoCaixaDTO> movimentos, String tipo) {
        return movimentos.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .map(MovimentoCaixaDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
