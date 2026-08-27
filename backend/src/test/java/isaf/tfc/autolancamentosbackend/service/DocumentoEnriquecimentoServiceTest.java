package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Peça "DocumentoEnriquecimentoService" do desenvolvimento incremental —
 * testa isoladamente (repositórios mockados) a conversão
 * DocumentoContabilistico → DocumentoResponseDTO, incluindo o cálculo do
 * estado ("Pendente"/"Analisado"/"Aprovado"/"Rejeitado") a partir da
 * Sugestao mais recente. Auditoria de performance: converterTodos() usa
 * agora findByDocumentoIdIn em lote (ver SugestaoRepository) em vez de uma
 * query por documento — este teste cobre esse caminho batch diretamente.
 */
class DocumentoEnriquecimentoServiceTest {

    private EntidadeRepository entidadeRepository;
    private SugestaoRepository sugestaoRepository;
    private DocumentoEnriquecimentoService service;

    @BeforeEach
    void setUp() {
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        service = new DocumentoEnriquecimentoService(entidadeRepository, sugestaoRepository);
    }

    private DocumentoContabilistico documento(Long id, Long entidadeId) {
        DocumentoContabilistico doc = new DocumentoContabilistico();
        doc.setId(id);
        doc.setNomeFicheiro("fatura.png");
        doc.setTipoConteudo("image/png");
        doc.setDataUpload(LocalDateTime.now());
        doc.setEntidadeId(entidadeId);
        doc.setConteudo(new byte[]{1, 2, 3});
        return doc;
    }

    private Sugestao sugestao(Long documentoId, EstadoSugestao estado, LocalDateTime dataCriacao) {
        Sugestao s = new Sugestao();
        s.setDocumentoId(documentoId);
        s.setEstado(estado);
        s.setDataCriacao(dataCriacao);
        return s;
    }

    @Test
    void converter_documentoSemSugestao_ficaPendente() {
        DocumentoContabilistico doc = documento(1L, null);
        when(sugestaoRepository.findByDocumentoIdIn(List.of(1L))).thenReturn(List.of());

        DocumentoResponseDTO dto = service.converter(doc);

        assertThat(dto.getEstado()).isEqualTo("Pendente");
        assertThat(dto.getEntidadeNome()).isNull();
        assertThat(dto.getTamanho()).isEqualTo(3);
    }

    @Test
    void converter_sugestaoAprovada_ficaAprovadoEIncluiNomeENifDaEntidade() {
        Entidade entidade = new Entidade(5L, "Sonangol Distribuidora Lda", "5417002619", TipoEntidade.FORNECEDOR);
        DocumentoContabilistico doc = documento(2L, 5L);
        when(entidadeRepository.findAllById(List.of(5L))).thenReturn(List.of(entidade));
        when(sugestaoRepository.findByDocumentoIdIn(List.of(2L)))
                .thenReturn(List.of(sugestao(2L, EstadoSugestao.APROVADA, LocalDateTime.now())));

        DocumentoResponseDTO dto = service.converter(doc);

        assertThat(dto.getEstado()).isEqualTo("Aprovado");
        assertThat(dto.getEntidadeNome()).isEqualTo("Sonangol Distribuidora Lda");
        assertThat(dto.getEntidadeNif()).isEqualTo("5417002619");
    }

    @Test
    void converterTodos_variosDocumentos_umaSoChamadaEmLoteAoSugestaoRepository() {
        DocumentoContabilistico doc1 = documento(10L, null);
        DocumentoContabilistico doc2 = documento(11L, null);
        when(sugestaoRepository.findByDocumentoIdIn(List.of(10L, 11L))).thenReturn(List.of(
                sugestao(10L, EstadoSugestao.REJEITADA, LocalDateTime.now()),
                sugestao(11L, EstadoSugestao.PENDENTE, LocalDateTime.now())
        ));

        List<DocumentoResponseDTO> resultado = service.converterTodos(List.of(doc1, doc2));

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getEstado()).isEqualTo("Rejeitado");
        assertThat(resultado.get(1).getEstado()).isEqualTo("Analisado");
        // Exactamente uma chamada em lote, nunca uma por documento (N+1).
        Mockito.verify(sugestaoRepository, Mockito.times(1)).findByDocumentoIdIn(Mockito.any());
    }

    @Test
    void converterTodos_documentoComVariasSugestoes_usaAMaisRecentePorDataCriacao() {
        DocumentoContabilistico doc = documento(20L, null);
        when(sugestaoRepository.findByDocumentoIdIn(List.of(20L))).thenReturn(List.of(
                sugestao(20L, EstadoSugestao.REJEITADA, LocalDateTime.now().minusDays(2)),
                sugestao(20L, EstadoSugestao.APROVADA, LocalDateTime.now())
        ));

        List<DocumentoResponseDTO> resultado = service.converterTodos(List.of(doc));

        assertThat(resultado.get(0).getEstado()).isEqualTo("Aprovado");
    }

    @Test
    void converterTodos_listaVazia_naoChamaOsRepositorios() {
        List<DocumentoResponseDTO> resultado = service.converterTodos(List.of());

        assertThat(resultado).isEmpty();
    }
}
