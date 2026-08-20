package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DadoGraficoMensalDTO;
import isaf.tfc.autolancamentosbackend.dto.DashboardResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.DocumentoRecenteDTO;
import isaf.tfc.autolancamentosbackend.dto.KpiDashboardDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Agrega dados já existentes (Lancamento, Sugestao, DocumentoContabilistico)
 * para o Dashboard — sem tabelas novas, sem duplicar dados.
 *
 * Classificação Receitas/Despesas: usa a classe do código de conta do
 * Decreto 82/01 (ver pgc.py) — classe 6 = Proveitos (receitas), classe 7 =
 * Custos (despesas). Só funciona para lançamentos cujas contas seguem essa
 * numeração (o fluxo automático já usa-a; lançamentos manuais só entram
 * corretamente no gráfico se a mesma numeração for usada — ver nota sobre
 * unificar o seletor de contas do diálogo "Novo Lançamento" com o
 * /api/contas real).
 */
@Service
public class DashboardService {

    private static final String[] MESES_PT = {
            "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private final LancamentoRepository lancamentoRepository;
    private final SugestaoRepository sugestaoRepository;
    private final DocumentoRepository documentoRepository;

    public DashboardService(LancamentoRepository lancamentoRepository,
                             SugestaoRepository sugestaoRepository,
                             DocumentoRepository documentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.documentoRepository = documentoRepository;
    }

    public DashboardResponseDTO obterDashboard(int mesesGrafico, int limiteRecentes) {
        List<Lancamento> lancamentos = lancamentoRepository.findAll();
        List<Sugestao> sugestoes = sugestaoRepository.findAll();

        return new DashboardResponseDTO(
                calcularKpis(lancamentos, sugestoes),
                calcularGraficoMensal(lancamentos, mesesGrafico),
                listarRecentes(lancamentos, limiteRecentes)
        );
    }

    private KpiDashboardDTO calcularKpis(List<Lancamento> lancamentos, List<Sugestao> sugestoes) {
        LocalDate hoje = LocalDate.now();

        long documentosImportadosHoje = documentoRepository.findAll().stream()
                .filter(d -> d.getDataUpload() != null && d.getDataUpload().toLocalDate().equals(hoje))
                .count();

        long sugestoesPendentes = sugestoes.stream()
                .filter(s -> s.getEstado() == EstadoSugestao.PENDENTE)
                .count();

        long lancamentosAprovadosMes = lancamentos.stream()
                .filter(l -> l.getEstado() == EstadoLancamento.VALIDADO)
                .filter(l -> l.getData() != null
                        && l.getData().getMonth() == hoje.getMonth()
                        && l.getData().getYear() == hoje.getYear())
                .count();

        long aprovadas = sugestoes.stream().filter(s -> s.getEstado() == EstadoSugestao.APROVADA).count();
        long rejeitadas = sugestoes.stream().filter(s -> s.getEstado() == EstadoSugestao.REJEITADA).count();

        // "Taxa de acerto": % de sugestões da IA aprovadas sem rejeição. Ainda
        // não existe um fluxo de "rejeitar sugestão" no AnaliseController, por
        // isso este número fica sempre perto de 100% por agora — não é uma
        // métrica de precisão validada por humanos, é a taxa de aprovação.
        double precisaoIA = (aprovadas + rejeitadas) == 0
                ? 100.0
                : (aprovadas * 100.0) / (aprovadas + rejeitadas);

        return new KpiDashboardDTO(documentosImportadosHoje, sugestoesPendentes, lancamentosAprovadosMes, precisaoIA);
    }

    private List<DadoGraficoMensalDTO> calcularGraficoMensal(List<Lancamento> lancamentos, int meses) {
        YearMonth atual = YearMonth.now();
        List<DadoGraficoMensalDTO> resultado = new ArrayList<>();

        for (int i = meses - 1; i >= 0; i--) {
            YearMonth alvo = atual.minusMonths(i);
            BigDecimal receitas = BigDecimal.ZERO;
            BigDecimal despesas = BigDecimal.ZERO;

            for (Lancamento lancamento : lancamentos) {
                if (lancamento.getData() == null || !YearMonth.from(lancamento.getData()).equals(alvo)) {
                    continue;
                }
                for (LinhaLancamento linha : lancamento.getLinhas()) {
                    String conta = linha.getConta();
                    if (conta == null) {
                        continue;
                    }
                    if (conta.startsWith("6") && linha.getCredito() != null) {
                        receitas = receitas.add(linha.getCredito());
                    } else if (conta.startsWith("7") && linha.getDebito() != null) {
                        despesas = despesas.add(linha.getDebito());
                    }
                }
            }

            resultado.add(new DadoGraficoMensalDTO(MESES_PT[alvo.getMonthValue() - 1], receitas, despesas));
        }

        return resultado;
    }

    private List<DocumentoRecenteDTO> listarRecentes(List<Lancamento> lancamentos, int limite) {
        return lancamentos.stream()
                .filter(l -> l.getData() != null)
                .sorted(Comparator.comparing(Lancamento::getData).reversed())
                .limit(limite)
                .map(this::converterParaRecente)
                .toList();
    }

    private DocumentoRecenteDTO converterParaRecente(Lancamento lancamento) {
        // findFirst() chama Optional.of() internamente, que rebenta com
        // null — algumas linhas antigas (de testes antes da correção do
        // categoriaContabil) têm conta null, por isso o filtro extra é
        // necessário, não só cosmético.
        String contaDebito = lancamento.getLinhas().stream()
                .filter(l -> l.getDebito() != null)
                .map(LinhaLancamento::getConta)
                .filter(Objects::nonNull)
                .findFirst().orElse("—");

        String contaCredito = lancamento.getLinhas().stream()
                .filter(l -> l.getCredito() != null)
                .map(LinhaLancamento::getConta)
                .filter(Objects::nonNull)
                .findFirst().orElse("—");

        BigDecimal valor = lancamento.getLinhas().stream()
                .map(LinhaLancamento::getDebito)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DocumentoRecenteDTO(
                String.valueOf(lancamento.getId()),
                lancamento.getDescricao(),
                contaDebito + " → " + contaCredito,
                lancamento.getData().toString(),
                mapEstado(lancamento.getEstado()),
                valor,
                lancamento.getOrigem() == OrigemLancamento.AUTOMATICO ? "ia" : "manual"
        );
    }

    private String mapEstado(EstadoLancamento estado) {
        return switch (estado) {
            case VALIDADO -> "aprovado";
            case CANCELADO -> "rejeitado";
            case PENDENTE -> "pendente";
            // Auditoria C03: ainda ativo (ver BalanceteService), mas o
            // frontend (Dashboard.tsx) só conhece os 3 estados originais —
            // "pendente" é a aproximação mais próxima enquanto não tem um
            // badge dedicado.
            case CANCELAMENTO_PENDENTE -> "pendente";
        };
    }
}
