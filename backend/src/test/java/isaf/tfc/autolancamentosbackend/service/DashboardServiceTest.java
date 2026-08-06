package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DashboardResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.DocumentoRecenteDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Peça do bloco "Dashboard": DashboardService isolado (repositórios
 * mockados) — agrega Lancamento/Sugestao/DocumentoContabilistico em KPIs,
 * gráfico mensal (classificação por classe de conta do PGC-AO: 6 =
 * receitas, 7 = despesas) e lista de recentes. Nunca tinha sido testado,
 * apesar de alimentar diretamente os números mostrados no Dashboard.
 */
class DashboardServiceTest {

    private LancamentoRepository lancamentoRepository;
    private SugestaoRepository sugestaoRepository;
    private DocumentoRepository documentoRepository;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        service = new DashboardService(lancamentoRepository, sugestaoRepository, documentoRepository);
    }

    // --- KPIs -------------------------------------------------------------

    @Test
    void kpis_contaApenasDocumentosCarregadosHoje() {
        DocumentoContabilistico hoje = documento(LocalDateTime.now());
        DocumentoContabilistico ontem = documento(LocalDateTime.now().minusDays(1));
        when(documentoRepository.findAll()).thenReturn(List.of(hoje, ontem));
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        DashboardResponseDTO resposta = service.obterDashboard(6, 5);

        assertThat(resposta.getKpis().getDocumentosImportados()).isEqualTo(1);
    }

    @Test
    void kpis_contaApenasSugestoesPendentes() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of(
                sugestaoComEstado(EstadoSugestao.PENDENTE),
                sugestaoComEstado(EstadoSugestao.PENDENTE),
                sugestaoComEstado(EstadoSugestao.APROVADA)
        ));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        DashboardResponseDTO resposta = service.obterDashboard(6, 5);

        assertThat(resposta.getKpis().getSugestoesPendentes()).isEqualTo(2);
    }

    @Test
    void kpis_lancamentosAprovados_soContaValidadosDoMesAtual() {
        LocalDate hoje = LocalDate.now();
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamento(EstadoLancamento.VALIDADO, hoje, OrigemLancamento.AUTOMATICO),
                lancamento(EstadoLancamento.PENDENTE, hoje, OrigemLancamento.AUTOMATICO),
                lancamento(EstadoLancamento.VALIDADO, hoje.minusMonths(2), OrigemLancamento.AUTOMATICO)
        ));

        DashboardResponseDTO resposta = service.obterDashboard(6, 5);

        assertThat(resposta.getKpis().getLancamentosAprovados()).isEqualTo(1);
    }

    @Test
    void kpis_precisaoIA_semSugestoesDecididas_ehCem() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of(sugestaoComEstado(EstadoSugestao.PENDENTE)));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        DashboardResponseDTO resposta = service.obterDashboard(6, 5);

        assertThat(resposta.getKpis().getPrecisaoIA()).isEqualTo(100.0);
    }

    @Test
    void kpis_precisaoIA_calculaPercentagemDeAprovadasSobreDecididas() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of(
                sugestaoComEstado(EstadoSugestao.APROVADA),
                sugestaoComEstado(EstadoSugestao.APROVADA),
                sugestaoComEstado(EstadoSugestao.APROVADA),
                sugestaoComEstado(EstadoSugestao.REJEITADA)
        ));
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        DashboardResponseDTO resposta = service.obterDashboard(6, 5);

        assertThat(resposta.getKpis().getPrecisaoIA()).isEqualTo(75.0);
    }

    // --- Gráfico mensal -----------------------------------------------------

    @Test
    void graficoMensal_classificaContaClasse6ComoReceitaEClasse7ComoDespesa() {
        Lancamento lancamento = lancamento(EstadoLancamento.VALIDADO, LocalDate.now(), OrigemLancamento.AUTOMATICO);
        lancamento.getLinhas().add(linha("61", null, new BigDecimal("100000.00")));
        lancamento.getLinhas().add(linha("71", new BigDecimal("30000.00"), null));
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(lancamento));

        DashboardResponseDTO resposta = service.obterDashboard(1, 5);

        assertThat(resposta.getGraficoMensal()).hasSize(1);
        assertThat(resposta.getGraficoMensal().get(0).getReceitas()).isEqualByComparingTo("100000.00");
        assertThat(resposta.getGraficoMensal().get(0).getDespesas()).isEqualByComparingTo("30000.00");
    }

    @Test
    void graficoMensal_ignoraLinhaComContaNula_naoLancaExcecao() {
        Lancamento lancamento = lancamento(EstadoLancamento.VALIDADO, LocalDate.now(), OrigemLancamento.AUTOMATICO);
        lancamento.getLinhas().add(linha(null, new BigDecimal("5000.00"), null));
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(lancamento));

        DashboardResponseDTO resposta = service.obterDashboard(1, 5);

        assertThat(resposta.getGraficoMensal().get(0).getReceitas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resposta.getGraficoMensal().get(0).getDespesas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void graficoMensal_devolveOsMesesPedidosPorOrdemCronologica() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        DashboardResponseDTO resposta = service.obterDashboard(3, 5);

        assertThat(resposta.getGraficoMensal()).hasSize(3);
        YearMonth atual = YearMonth.now();
        String[] mesesPt = { "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez" };
        assertThat(resposta.getGraficoMensal().get(2).getMes())
                .isEqualTo(mesesPt[atual.getMonthValue() - 1]);
    }

    // --- Lançamentos recentes ------------------------------------------------

    @Test
    void recentes_ordenaPorDataDescendenteELimita() {
        LocalDate hoje = LocalDate.now();
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinha(hoje.minusDays(5), EstadoLancamento.VALIDADO, OrigemLancamento.MANUAL),
                lancamentoComLinha(hoje, EstadoLancamento.VALIDADO, OrigemLancamento.AUTOMATICO),
                lancamentoComLinha(hoje.minusDays(2), EstadoLancamento.PENDENTE, OrigemLancamento.AUTOMATICO)
        ));

        DashboardResponseDTO resposta = service.obterDashboard(1, 2);

        List<DocumentoRecenteDTO> recentes = resposta.getDocumentosRecentes();
        assertThat(recentes).hasSize(2);
        assertThat(recentes.get(0).getData()).isEqualTo(hoje.toString());
        assertThat(recentes.get(1).getData()).isEqualTo(hoje.minusDays(2).toString());
    }

    @Test
    void recentes_mapeiaEstadoEOrigemCorretamente() {
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(lancamentoRepository.findAll()).thenReturn(List.of(
                lancamentoComLinha(LocalDate.now(), EstadoLancamento.CANCELADO, OrigemLancamento.MANUAL)
        ));

        DashboardResponseDTO resposta = service.obterDashboard(1, 5);

        DocumentoRecenteDTO recente = resposta.getDocumentosRecentes().get(0);
        assertThat(recente.getEstado()).isEqualTo("rejeitado");
        assertThat(recente.getOrigem()).isEqualTo("manual");
    }

    // --- helpers ------------------------------------------------------------

    private DocumentoContabilistico documento(LocalDateTime dataUpload) {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setDataUpload(dataUpload);
        return documento;
    }

    private Sugestao sugestaoComEstado(EstadoSugestao estado) {
        Sugestao sugestao = new Sugestao();
        sugestao.setEstado(estado);
        return sugestao;
    }

    private Lancamento lancamento(EstadoLancamento estado, LocalDate data, OrigemLancamento origem) {
        Lancamento lancamento = new Lancamento();
        lancamento.setEstado(estado);
        lancamento.setData(data);
        lancamento.setOrigem(origem);
        lancamento.setDescricao("Teste dashboard");
        return lancamento;
    }

    private Lancamento lancamentoComLinha(LocalDate data, EstadoLancamento estado, OrigemLancamento origem) {
        Lancamento lancamento = lancamento(estado, data, origem);
        lancamento.getLinhas().add(linha("31", new BigDecimal("1000.00"), null));
        lancamento.getLinhas().add(linha("61", null, new BigDecimal("1000.00")));
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
