package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.LivroRazaoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.MovimentoRazaoDTO;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Fase 18 do plano de 20 fases — "auditor: livro razão" (consulta por
 * conta individual; até aqui só existia o Balancete, agregado). Lista
 * cada movimento da conta pedida, em ordem cronológica, com saldo
 * acumulado — a mesma simplificação já assumida em BalanceteService
 * ("não existe saldo anterior separado do movimento do período": este
 * sistema ainda não tem períodos contabilísticos fechados, por isso o
 * saldo acumulado do razão começa sempre em zero no início do
 * intervalo pedido, nunca transporta um saldo de um período anterior
 * não filtrado).
 *
 * Reutiliza BalanceteService.lancamentosValidadosNoIntervalo() — mesma
 * disciplina de fonte única das Fases 6/7/13/17: nunca volta a
 * interrogar o LancamentoRepository diretamente.
 */
@Service
public class LivroRazaoService {

    private final BalanceteService balanceteService;
    private final PlanoContasClient planoContasClient;

    public LivroRazaoService(BalanceteService balanceteService, PlanoContasClient planoContasClient) {
        this.balanceteService = balanceteService;
        this.planoContasClient = planoContasClient;
    }

    public LivroRazaoResponseDTO gerarLivroRazao(String conta, LocalDate inicio, LocalDate fim) {
        List<Lancamento> lancamentos = balanceteService.lancamentosValidadosNoIntervalo(inicio, fim);

        record MovimentoBruto(Long lancamentoId, LocalDate data, String descricao, BigDecimal debito, BigDecimal credito) {}

        List<MovimentoBruto> brutos = lancamentos.stream()
                .flatMap(l -> l.getLinhas().stream()
                        .filter(linha -> conta.equals(linha.getConta()))
                        .map(linha -> new MovimentoBruto(l.getId(), l.getData(), descricaoDoMovimento(l, linha), linha.getDebito(), linha.getCredito())))
                .sorted(Comparator.comparing(MovimentoBruto::data, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MovimentoBruto::lancamentoId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<MovimentoRazaoDTO> movimentos = new java.util.ArrayList<>();
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal totalDebito = BigDecimal.ZERO;
        BigDecimal totalCredito = BigDecimal.ZERO;

        for (MovimentoBruto m : brutos) {
            BigDecimal debito = m.debito() != null ? m.debito() : BigDecimal.ZERO;
            BigDecimal credito = m.credito() != null ? m.credito() : BigDecimal.ZERO;
            saldo = saldo.add(debito).subtract(credito);
            totalDebito = totalDebito.add(debito);
            totalCredito = totalCredito.add(credito);
            movimentos.add(new MovimentoRazaoDTO(m.lancamentoId(), m.data(), m.descricao(), m.debito(), m.credito(), saldo));
        }

        return new LivroRazaoResponseDTO(conta, nomeDaConta(conta), inicio, fim, movimentos, totalDebito, totalCredito, saldo);
    }

    // A descrição da linha (ex.: "IVA dedutível") é mais específica do que
    // a do lançamento (ex.: "Fatura de compra"), mas fica vazia em
    // lançamentos mais antigos — cai para a do lançamento nesse caso.
    private String descricaoDoMovimento(Lancamento lancamento, LinhaLancamento linha) {
        return (linha.getDescricao() != null && !linha.getDescricao().isBlank())
                ? linha.getDescricao()
                : lancamento.getDescricao();
    }

    private String nomeDaConta(String codigo) {
        return planoContasClient.listar().stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .map(ContaDTO::getNome)
                .findFirst()
                .orElse(codigo);
    }
}
