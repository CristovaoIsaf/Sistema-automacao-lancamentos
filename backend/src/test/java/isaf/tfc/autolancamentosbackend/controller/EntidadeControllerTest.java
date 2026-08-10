package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.EntidadeDossieDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import isaf.tfc.autolancamentosbackend.service.DocumentoEnriquecimentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Fase 10 do plano de 20 fases — "dossiê da entidade": EntidadeController
 * isolado (repositórios mockados). DocumentoEnriquecimentoService real
 * (não mockado) reutilizando os mesmos mocks — a mesma técnica já usada em
 * DocumentoControllerTest, para não duplicar a lógica de enriquecimento.
 */
class EntidadeControllerTest {

    private EntidadeRepository entidadeRepository;
    private DocumentoRepository documentoRepository;
    private SugestaoRepository sugestaoRepository;
    private EntidadeController controller;

    @BeforeEach
    void setUp() {
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);

        DocumentoEnriquecimentoService enriquecimentoService =
                new DocumentoEnriquecimentoService(entidadeRepository, sugestaoRepository);
        controller = new EntidadeController(entidadeRepository, documentoRepository, enriquecimentoService);
    }

    @Test
    void dossie_entidadeComDocumentos_devolveDadosDaEntidadeETodosOsDocumentos() {
        Entidade entidade = new Entidade(7L, "Fornecedor XPTO", "5417000111", TipoEntidade.FORNECEDOR);
        when(entidadeRepository.findById(7L)).thenReturn(Optional.of(entidade));
        when(entidadeRepository.findAllById(List.of(7L))).thenReturn(List.of(entidade));

        DocumentoContabilistico doc1 = documentoComId(1L, 7L);
        DocumentoContabilistico doc2 = documentoComId(2L, 7L);
        when(documentoRepository.findByEntidadeId(7L)).thenReturn(List.of(doc1, doc2));
        when(sugestaoRepository.findAllByDocumentoId(1L)).thenReturn(List.of());
        when(sugestaoRepository.findAllByDocumentoId(2L)).thenReturn(List.of());

        EntidadeDossieDTO dossie = controller.dossie(7L).getBody();

        assertThat(dossie.getNome()).isEqualTo("Fornecedor XPTO");
        assertThat(dossie.getNif()).isEqualTo("5417000111");
        assertThat(dossie.getTipo()).isEqualTo("FORNECEDOR");
        assertThat(dossie.getDocumentos()).hasSize(2);
    }

    @Test
    void dossie_entidadeInexistente_lancaExcecao() {
        when(entidadeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.dossie(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não encontrada");
    }

    private DocumentoContabilistico documentoComId(Long id, Long entidadeId) {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setId(id);
        documento.setNomeFicheiro("fatura-" + id + ".png");
        documento.setTipoConteudo("image/png");
        documento.setDataUpload(LocalDateTime.now());
        documento.setEntidadeId(entidadeId);
        return documento;
    }
}
