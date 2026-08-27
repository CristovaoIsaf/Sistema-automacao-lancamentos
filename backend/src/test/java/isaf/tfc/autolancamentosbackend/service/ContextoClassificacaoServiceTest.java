package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContextoClassificacaoDTO;
import isaf.tfc.autolancamentosbackend.dto.EmpresaDTO;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Peça "ContextoClassificacaoService" (Context Engine, Fase 3) do
 * desenvolvimento incremental — testa isoladamente (dependências mockadas)
 * a agregação de empresa + entidade + histórico, sem nenhuma chamada real a
 * IA/FastAPI (esta classe explicitamente "não decide nada sozinha").
 */
class ContextoClassificacaoServiceTest {

    private EmpresaService empresaService;
    private EntidadeRepository entidadeRepository;
    private SugestaoRepository sugestaoRepository;
    private ContextoClassificacaoService service;

    @BeforeEach
    void setUp() {
        empresaService = Mockito.mock(EmpresaService.class);
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        service = new ContextoClassificacaoService(empresaService, entidadeRepository, sugestaoRepository);

        when(empresaService.obterEmpresa()).thenReturn(new EmpresaDTO(
                1L, "Empresa Teste", "5417002619", "e@e.ao", "Rua X", "900000000",
                "Comércio a retalho", "Revenda de mercadorias", "AOA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
    }

    @Test
    void construirContexto_semEntidadeId_devolveSoDadosDaEmpresaSemHistorico() {
        ContextoClassificacaoDTO contexto = service.construirContexto(null);

        assertThat(contexto.getVersao()).isEqualTo(ContextoClassificacaoDTO.VERSAO);
        assertThat(contexto.getEmpresaAtividadeEconomica()).isEqualTo("Comércio a retalho");
        assertThat(contexto.getEmpresaMoeda()).isEqualTo("AOA");
        assertThat(contexto.getEntidadeId()).isNull();
        assertThat(contexto.getEntidadeNome()).isNull();
        assertThat(contexto.getHistoricoTiposOperacaoRecentes()).isEmpty();
        verify(entidadeRepository, never()).findById(any());
        verify(sugestaoRepository, never()).findRecentesPorEntidade(any(), any());
    }

    @Test
    void construirContexto_comEntidadeConhecida_incluiDadosDaEntidadeEHistorico() {
        Entidade entidade = new Entidade(5L, "Sonangol Distribuidora Lda", "5417002619", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findById(5L)).thenReturn(Optional.of(entidade));

        Sugestao s1 = new Sugestao();
        s1.setTipoDocumento("compra_mercadoria");
        Sugestao s2 = new Sugestao();
        s2.setTipoDocumento("compra_servico");
        when(sugestaoRepository.findRecentesPorEntidade(eq(5L), any(Pageable.class))).thenReturn(List.of(s1, s2));

        ContextoClassificacaoDTO contexto = service.construirContexto(5L);

        assertThat(contexto.getEntidadeId()).isEqualTo(5L);
        assertThat(contexto.getEntidadeNome()).isEqualTo("Sonangol Distribuidora Lda");
        assertThat(contexto.getEntidadeNif()).isEqualTo("5417002619");
        assertThat(contexto.getEntidadeTipo()).isEqualTo("FORNECEDOR");
        assertThat(contexto.getHistoricoTiposOperacaoRecentes()).containsExactly("compra_mercadoria", "compra_servico");
    }

    @Test
    void construirContexto_entidadeIdQueNaoExiste_naoRebentaEDevolveSemEntidade() {
        when(entidadeRepository.findById(999L)).thenReturn(Optional.empty());

        ContextoClassificacaoDTO contexto = service.construirContexto(999L);

        assertThat(contexto.getEntidadeId()).isNull();
        assertThat(contexto.getHistoricoTiposOperacaoRecentes()).isEmpty();
    }

    @Test
    void construirContexto_historicoComTiposNulos_sãoFiltrados() {
        Entidade entidade = new Entidade(5L, "Alguém", "5417002619", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findById(5L)).thenReturn(Optional.of(entidade));

        Sugestao comTipo = new Sugestao();
        comTipo.setTipoDocumento("compra_mercadoria");
        Sugestao semTipo = new Sugestao();
        semTipo.setTipoDocumento(null);
        when(sugestaoRepository.findRecentesPorEntidade(eq(5L), any(Pageable.class)))
                .thenReturn(List.of(comTipo, semTipo));

        ContextoClassificacaoDTO contexto = service.construirContexto(5L);

        assertThat(contexto.getHistoricoTiposOperacaoRecentes()).containsExactly("compra_mercadoria");
    }
}
