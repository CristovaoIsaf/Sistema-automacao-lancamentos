package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import isaf.tfc.autolancamentosbackend.model.OrigemLancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Fase 14 do plano de 20 fases — "Notas às Contas": NotaContaService
 * isolado (repositórios mockados). Cobre em particular a resolução de
 * nome/natureza via PlanoContasClient (aditivo desta fase — antes a nota
 * só devolvia o código da conta, sem nome nem natureza).
 */
class NotaContaServiceTest {

    private LancamentoRepository lancamentoRepository;
    private SugestaoRepository sugestaoRepository;
    private DocumentoRepository documentoRepository;
    private EntidadeRepository entidadeRepository;
    private PlanoContasClient planoContasClient;
    private NotaContaService service;

    @BeforeEach
    void setUp() {
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        planoContasClient = Mockito.mock(PlanoContasClient.class);
        when(planoContasClient.listar()).thenReturn(List.of(
                new ContaDTO("32", "Fornecedores", "3", null, "CREDORA"),
                new ContaDTO("31", "Clientes", "3", null, "DEVEDORA")
        ));
        when(sugestaoRepository.findAll()).thenReturn(List.of());
        when(documentoRepository.findAll()).thenReturn(List.of());
        when(entidadeRepository.findAll()).thenReturn(List.of());

        service = new NotaContaService(lancamentoRepository, sugestaoRepository, documentoRepository, entidadeRepository, planoContasClient);
    }

    @Test
    void obterNota_resolveNomeENaturezaDaContaAPartirDoPlanoDeContas() {
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        NotaContaResponseDTO nota = service.obterNota("32", null, null);

        assertThat(nota.getNomeConta()).isEqualTo("Fornecedores");
        assertThat(nota.getNatureza()).isEqualTo("CREDORA");
    }

    @Test
    void obterNota_contaDesconhecidaDoPlanoDeContas_ficaComNomeENaturezaNulos() {
        when(lancamentoRepository.findAll()).thenReturn(List.of());

        NotaContaResponseDTO nota = service.obterNota("99.9.9", null, null);

        assertThat(nota.getNomeConta()).isNull();
        assertThat(nota.getNatureza()).isNull();
    }

    @Test
    void obterNota_agrupaMovimentosPorEntidadeViaDocumentoESugestao() {
        Entidade entidade = new Entidade(5L, "Fornecedor XPTO", "5417000111", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findAll()).thenReturn(List.of(entidade));

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setId(10L);
        documento.setNomeFicheiro("fatura.png");
        documento.setEntidadeId(5L);
        when(documentoRepository.findAll()).thenReturn(List.of(documento));

        Lancamento lancamento = lancamentoComLinha("32", new BigDecimal("5000.00"), null, LocalDate.now());
        lancamento.setId(1L);
        when(lancamentoRepository.findAll()).thenReturn(List.of(lancamento));

        Sugestao sugestao = new Sugestao();
        sugestao.setLancamentoId(1L);
        sugestao.setDocumentoId(10L);
        when(sugestaoRepository.findAll()).thenReturn(List.of(sugestao));

        NotaContaResponseDTO nota = service.obterNota("32", null, null);

        assertThat(nota.getPorEntidade()).hasSize(1);
        assertThat(nota.getPorEntidade().get(0).getEntidade()).isEqualTo("Fornecedor XPTO");
        assertThat(nota.getPorEntidade().get(0).getSubtotalDebito()).isEqualByComparingTo("5000.00");
        assertThat(nota.getTotalDebito()).isEqualByComparingTo("5000.00");
    }

    @Test
    void obterNota_movimentoSemDocumentoAssociado_ficaAgrupadoComoSemOrigemDocumental() {
        Lancamento lancamento = lancamentoComLinha("32", null, new BigDecimal("2000.00"), LocalDate.now());
        lancamento.setId(2L);
        when(lancamentoRepository.findAll()).thenReturn(List.of(lancamento));

        NotaContaResponseDTO nota = service.obterNota("32", null, null);

        assertThat(nota.getPorEntidade()).hasSize(1);
        assertThat(nota.getPorEntidade().get(0).getEntidade()).isEqualTo("Sem origem documental associada");
        assertThat(nota.getPorEntidade().get(0).getSubtotalCredito()).isEqualByComparingTo("2000.00");
    }

    @Test
    void obterNota_subContaAgrupaSobAContaMae() {
        Lancamento lancamento = lancamentoComLinha("75.2.11", new BigDecimal("1000.00"), null, LocalDate.now());
        lancamento.setId(3L);
        when(lancamentoRepository.findAll()).thenReturn(List.of(lancamento));

        NotaContaResponseDTO nota = service.obterNota("75", null, null);

        assertThat(nota.getTotalDebito()).isEqualByComparingTo("1000.00");
    }

    private Lancamento lancamentoComLinha(String conta, BigDecimal debito, BigDecimal credito, LocalDate data) {
        Lancamento lancamento = new Lancamento();
        lancamento.setData(data);
        lancamento.setEstado(EstadoLancamento.VALIDADO);
        lancamento.setOrigem(OrigemLancamento.AUTOMATICO);
        lancamento.setDescricao("Teste");
        LinhaLancamento linha = new LinhaLancamento();
        linha.setConta(conta);
        linha.setDebito(debito);
        linha.setCredito(credito);
        linha.setLancamento(lancamento);
        lancamento.getLinhas().add(linha);
        return lancamento;
    }
}
