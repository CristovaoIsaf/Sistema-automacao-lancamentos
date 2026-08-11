package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.BalanceteResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.BalancoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.DREResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaDemonstracaoDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fase 13 do plano de 20 fases — "garantir consistência entre lançamentos
 * → balancete → balanço → DRE": DRE e Balanço são construídos SÓ a partir
 * de BalanceteService.gerarBalancete() (a mesma soma débito/crédito por
 * conta, sobre os mesmos Lancamento VALIDADO) — nunca recalculam nada a
 * partir de Lancamento directamente, para nunca poderem divergir do
 * balancete. "Não criar cálculos independentes no frontend": antes desta
 * fase, Relatorios.tsx (DRE) e o balanço mostravam números 100% inventados
 * no componente React, sem nenhum endpoint a suportá-los.
 *
 * Fluxo de caixa e Livro Razão ficam FORA do âmbito desta fase — um fluxo
 * de caixa correto exige classificar cada movimento em
 * operacional/investimento/financiamento, uma modelação que este TFC (ver
 * pgc.py: "âmbito compra, venda, pagamentos") não suporta sem inventar
 * regras de classificação não verificadas.
 */
@Service
public class DemonstracoesFinanceirasService {

    // Classe 6 (Proveitos) — ver pgc.py.
    private static final Set<String> CLASSES_RECEITA = Set.of("6");

    // Classe 7 (Custos) + classe 2 (Existências — só a conta Compras/21 é
    // realmente usada pelos lançamentos automáticos, ver pgc_ao.
    // construir_lancamento). Tratada como gasto do período inteiro: este
    // TFC não modela stock/CMVC, por isso não há como separar "vendido"
    // de "em armazém" — decisão explícita, documentada também no DTO.
    private static final Set<String> CLASSES_GASTO = Set.of("7", "2");

    // Classe 3 (Terceiros: Clientes/Fornecedores/Estado) + classe 4 (Meios
    // monetários: Caixa/Depósitos) — as duas classes de Passivo/Ativo
    // deste PGC-AO reduzido. Excluir classes 2/6/7 do Balanço evita contar
    // o MESMO valor duas vezes (uma na DRE como gasto/receita do período,
    // outra aqui como "saldo" — este sistema não tem fecho de exercício/
    // encerramento de contas de resultados que evite essa sobreposição).
    private static final Set<String> CLASSES_BALANCO = Set.of("3", "4");

    // Classe 5 — Capital e Reservas (Fase 19 do plano de 20 fases).
    // Separada de CLASSES_BALANCO porque entra numa secção própria
    // (Capital Próprio), não em Ativo/Passivo.
    private static final String CLASSE_CAPITAL_PROPRIO = "5";

    private static final String NATUREZA_DEVEDORA = "DEVEDORA";
    private static final String NATUREZA_CREDORA = "CREDORA";

    private final BalanceteService balanceteService;
    private final PlanoContasClient planoContasClient;

    public DemonstracoesFinanceirasService(BalanceteService balanceteService, PlanoContasClient planoContasClient) {
        this.balanceteService = balanceteService;
        this.planoContasClient = planoContasClient;
    }

    public DREResponseDTO gerarDRE(LocalDate inicio, LocalDate fim) {
        BalanceteResponseDTO balancete = balanceteService.gerarBalancete(inicio, fim);
        Map<String, ContaDTO> contasPorCodigo = contasPorCodigo();

        List<LinhaDemonstracaoDTO> receitas = balancete.getLinhas().stream()
                .filter(l -> pertenceAClasse(l.getConta(), contasPorCodigo, CLASSES_RECEITA))
                .filter(l -> l.getCreditoAcumulado().signum() > 0)
                .map(l -> new LinhaDemonstracaoDTO(l.getConta(), l.getNome(), l.getCreditoAcumulado()))
                .toList();

        List<LinhaDemonstracaoDTO> gastos = balancete.getLinhas().stream()
                .filter(l -> pertenceAClasse(l.getConta(), contasPorCodigo, CLASSES_GASTO))
                .filter(l -> l.getDebitoAcumulado().signum() > 0)
                .map(l -> new LinhaDemonstracaoDTO(l.getConta(), l.getNome(), l.getDebitoAcumulado()))
                .toList();

        BigDecimal totalReceitas = somar(receitas);
        BigDecimal totalGastos = somar(gastos);

        return new DREResponseDTO(inicio, fim, receitas, totalReceitas, gastos, totalGastos, totalReceitas.subtract(totalGastos));
    }

    public BalancoResponseDTO gerarBalanco(LocalDate inicio, LocalDate fim) {
        BalanceteResponseDTO balancete = balanceteService.gerarBalancete(inicio, fim);
        Map<String, ContaDTO> contasPorCodigo = contasPorCodigo();

        List<LinhaDemonstracaoDTO> ativo = balancete.getLinhas().stream()
                .filter(l -> pertenceAClasse(l.getConta(), contasPorCodigo, CLASSES_BALANCO))
                .filter(l -> NATUREZA_DEVEDORA.equals(naturezaDaConta(l.getConta(), contasPorCodigo)))
                .filter(l -> l.getSaldoDevedor().signum() > 0)
                .map(l -> new LinhaDemonstracaoDTO(l.getConta(), l.getNome(), l.getSaldoDevedor()))
                .toList();

        List<LinhaDemonstracaoDTO> passivo = balancete.getLinhas().stream()
                .filter(l -> pertenceAClasse(l.getConta(), contasPorCodigo, CLASSES_BALANCO))
                .filter(l -> NATUREZA_CREDORA.equals(naturezaDaConta(l.getConta(), contasPorCodigo)))
                .filter(l -> l.getSaldoCredor().signum() > 0)
                .map(l -> new LinhaDemonstracaoDTO(l.getConta(), l.getNome(), l.getSaldoCredor()))
                .toList();

        List<LinhaDemonstracaoDTO> capitalProprio = new java.util.ArrayList<>(balancete.getLinhas().stream()
                .filter(l -> pertenceAClasse(l.getConta(), contasPorCodigo, Set.of(CLASSE_CAPITAL_PROPRIO)))
                .filter(l -> l.getSaldoCredor().signum() > 0)
                .map(l -> new LinhaDemonstracaoDTO(l.getConta(), l.getNome(), l.getSaldoCredor()))
                .toList());

        // Resultado do período (Fase 19): reutiliza gerarDRE — nunca
        // recalcula receitas/gastos aqui (mesma disciplina "fonte única"
        // já aplicada ao Balancete). "88" é o código real do Decreto 82/01
        // para "Resultado Líquido do Exercício" (classe 8) — usado só como
        // rótulo desta linha sintética, nunca lançado (este sistema não
        // tem fecho de exercício).
        BigDecimal resultadoExercicio = gerarDRE(inicio, fim).getResultadoLiquido();
        capitalProprio.add(new LinhaDemonstracaoDTO("88", "Resultado do Exercício", resultadoExercicio));

        BigDecimal totalAtivo = somar(ativo);
        BigDecimal totalPassivo = somar(passivo);
        BigDecimal totalCapitalProprio = somar(capitalProprio);

        return new BalancoResponseDTO(inicio, fim, ativo, totalAtivo, passivo, totalPassivo,
                capitalProprio, totalCapitalProprio, totalAtivo.subtract(totalPassivo.add(totalCapitalProprio)));
    }

    private Map<String, ContaDTO> contasPorCodigo() {
        return planoContasClient.listar().stream()
                .collect(Collectors.toMap(ContaDTO::getCodigo, Function.identity()));
    }

    private boolean pertenceAClasse(String codigo, Map<String, ContaDTO> contasPorCodigo, Set<String> classes) {
        ContaDTO conta = contasPorCodigo.get(codigo);
        return conta != null && classes.contains(conta.getClasse());
    }

    private String naturezaDaConta(String codigo, Map<String, ContaDTO> contasPorCodigo) {
        ContaDTO conta = contasPorCodigo.get(codigo);
        return conta != null ? conta.getNatureza() : null;
    }

    private BigDecimal somar(List<LinhaDemonstracaoDTO> linhas) {
        return linhas.stream().map(LinhaDemonstracaoDTO::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
