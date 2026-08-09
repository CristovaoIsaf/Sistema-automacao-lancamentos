package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AprovarSugestaoRequest;
import isaf.tfc.autolancamentosbackend.dto.LancamentoResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.EstadoLancamento;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Lancamento;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Peça 2 do bloco "Partidas dobradas / IVA": testa a orquestração de
 * AnaliseContabilService.aprovarSugestao isoladamente — repositórios
 * mockados, ObjectMapper real (é barato e evita mockar (des)serialização
 * JSON). Confirma que PartidasDobradas (peça 1, já validada) é mesmo
 * chamada e respeitada no caminho real do serviço.
 */
class AnaliseContabilServiceTest {

    private SugestaoRepository sugestaoRepository;
    private LancamentoRepository lancamentoRepository;
    private AnaliseContabilService service;

    @BeforeEach
    void setUp() {
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        // Simula a atribuição de id que a BD real faria no INSERT — sem isto,
        // salvo.getId() ficaria sempre null e sugestao.setLancamentoId(...)
        // nunca teria um valor para testar.
        when(lancamentoRepository.save(any())).thenAnswer(inv -> {
            Lancamento lancamento = inv.getArgument(0);
            lancamento.setId(999L);
            return lancamento;
        });
        when(sugestaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LancamentoEnriquecimentoService enriquecimentoService = Mockito.mock(LancamentoEnriquecimentoService.class);
        when(enriquecimentoService.converter(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            LancamentoResponseDTO dto = new LancamentoResponseDTO();
            dto.setId(l.getId());
            dto.setData(l.getData());
            dto.setDescricao(l.getDescricao());
            dto.setEstado(l.getEstado());
            dto.setOrigem(l.getOrigem());
            dto.setEditadoManualmente(l.getEditadoManualmente());
            dto.setLinhas(l.getLinhas().stream()
                    .map(linha -> new LinhaLancamentoDTO(linha.getConta(), linha.getDebito(), linha.getCredito(), linha.getDescricao()))
                    .toList());
            return dto;
        });

        service = new AnaliseContabilService(
                Mockito.mock(AnalisadorDocumentoIA.class),
                Mockito.mock(DocumentoRepository.class),
                sugestaoRepository,
                lancamentoRepository,
                Mockito.mock(EntidadeService.class),
                Mockito.mock(ContextoClassificacaoService.class),
                enriquecimentoService,
                new ObjectMapper()
        );
    }

    @Test
    void aprovarSugestao_jaAprovada_lancaExcecaoENaoGravaNadaDeNovo() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setEstado(EstadoSugestao.APROVADA);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        assertThatThrownBy(() -> service.aprovarSugestao(1L, 4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já foi aprovada");

        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void aprovarSugestao_semLinhasJson_criaUmaLinhaDeDebitoUnica() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setLinhasJson(null);
        sugestao.setCategoriaContabil("31");
        sugestao.setValor("50000.00");
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        LancamentoResponseDTO resposta = service.aprovarSugestao(1L, 4L);

        assertThat(resposta.getLinhas()).hasSize(1);
        assertThat(resposta.getLinhas().get(0).getConta()).isEqualTo("31");
        assertThat(resposta.getLinhas().get(0).getDebito()).isEqualByComparingTo("50000.00");
        assertThat(resposta.getLinhas().get(0).getCredito()).isNull();
    }

    @Test
    void aprovarSugestao_comLinhasJsonEquilibradas_criaTodasAsLinhasEMarcaSugestaoAprovada() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setLinhasJson("""
                [
                  {"conta":"31","nome":"Clientes","debito":"50000.00","credito":null},
                  {"conta":"61","nome":"Vendas","debito":null,"credito":"43859.65"},
                  {"conta":"34.5.2","nome":"IVA liquidado","debito":null,"credito":"6140.35"}
                ]
                """);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        LancamentoResponseDTO resposta = service.aprovarSugestao(1L, 4L);

        assertThat(resposta.getLinhas()).hasSize(3);
        assertThat(resposta.getEstado()).isEqualTo(EstadoLancamento.VALIDADO);

        ArgumentCaptor<Sugestao> sugestaoCapturada = ArgumentCaptor.forClass(Sugestao.class);
        verify(sugestaoRepository).save(sugestaoCapturada.capture());
        assertThat(sugestaoCapturada.getValue().getEstado()).isEqualTo(EstadoSugestao.APROVADA);
        assertThat(sugestaoCapturada.getValue().getLancamentoId()).isNotNull();
    }

    @Test
    void aprovarSugestao_comLinhasJsonDesequilibradas_lancaExcecaoENaoGravaLancamento() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setLinhasJson("""
                [
                  {"conta":"31","nome":"Clientes","debito":"50000.00","credito":null},
                  {"conta":"61","nome":"Vendas","debito":null,"credito":"40000.00"}
                ]
                """);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        assertThatThrownBy(() -> service.aprovarSugestao(1L, 4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não está equilibrado");

        verify(lancamentoRepository, never()).save(any());
        verify(sugestaoRepository, never()).save(any());
    }

    @Test
    void aprovarSugestao_comOverrideDeLinhas_usaAsLinhasDoRequestNaoAsDaSugestao() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setLinhasJson("""
                [{"conta":"99","nome":"Ignorada","debito":"1.00","credito":null}]
                """);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        AprovarSugestaoRequest request = new AprovarSugestaoRequest(
                List.of(
                        new LinhaLancamentoDTO("45", new java.math.BigDecimal("1000.00"), null, "Caixa"),
                        new LinhaLancamentoDTO("61", null, new java.math.BigDecimal("1000.00"), "Vendas")
                ),
                false
        );

        LancamentoResponseDTO resposta = service.aprovarSugestao(1L, 4L, request);

        assertThat(resposta.getLinhas()).hasSize(2);
        assertThat(resposta.getLinhas().get(0).getConta()).isEqualTo("45");
        assertThat(resposta.getEditadoManualmente()).isFalse();
    }

    @Test
    void aprovarSugestao_comOverrideEEditadoTrue_marcaLancamentoComoEditado() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setLinhasJson(null);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        AprovarSugestaoRequest request = new AprovarSugestaoRequest(
                List.of(
                        new LinhaLancamentoDTO("45", new java.math.BigDecimal("500.00"), null, "Caixa"),
                        new LinhaLancamentoDTO("61", null, new java.math.BigDecimal("500.00"), "Vendas")
                ),
                true
        );

        LancamentoResponseDTO resposta = service.aprovarSugestao(1L, 4L, request);

        assertThat(resposta.getEditadoManualmente()).isTrue();
    }

    @Test
    void aprovarSugestao_comOverrideDesequilibrado_lancaExcecaoENaoGrava() {
        Sugestao sugestao = sugestaoBase();
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        AprovarSugestaoRequest request = new AprovarSugestaoRequest(
                List.of(
                        new LinhaLancamentoDTO("45", new java.math.BigDecimal("500.00"), null, "Caixa"),
                        new LinhaLancamentoDTO("61", null, new java.math.BigDecimal("400.00"), "Vendas")
                ),
                false
        );

        assertThatThrownBy(() -> service.aprovarSugestao(1L, 4L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não está equilibrado");

        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void aprovarSugestao_jaRejeitada_lancaExcecaoENaoGravaNadaDeNovo() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setEstado(EstadoSugestao.REJEITADA);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        assertThatThrownBy(() -> service.aprovarSugestao(1L, 4L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("anulada");

        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void rejeitarSugestao_pendente_marcaComoRejeitadaSemCriarLancamento() {
        Sugestao sugestao = sugestaoBase();
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        service.rejeitarSugestao(1L);

        ArgumentCaptor<Sugestao> sugestaoCapturada = ArgumentCaptor.forClass(Sugestao.class);
        verify(sugestaoRepository).save(sugestaoCapturada.capture());
        assertThat(sugestaoCapturada.getValue().getEstado()).isEqualTo(EstadoSugestao.REJEITADA);
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void rejeitarSugestao_jaAprovada_lancaExcecaoENaoAltera() {
        Sugestao sugestao = sugestaoBase();
        sugestao.setEstado(EstadoSugestao.APROVADA);
        when(sugestaoRepository.findById(1L)).thenReturn(java.util.Optional.of(sugestao));

        assertThatThrownBy(() -> service.rejeitarSugestao(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("pendente");

        verify(sugestaoRepository, never()).save(any());
    }

    private Sugestao sugestaoBase() {
        Sugestao sugestao = new Sugestao();
        sugestao.setId(1L);
        sugestao.setDocumentoId(16L);
        sugestao.setDescricao("Combustivel para veiculos da empresa");
        sugestao.setEstado(EstadoSugestao.PENDENTE);
        return sugestao;
    }
}
