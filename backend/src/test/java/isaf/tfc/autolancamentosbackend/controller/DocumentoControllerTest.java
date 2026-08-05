package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.Entidade;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.TipoEntidade;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.EntidadeRepository;
import isaf.tfc.autolancamentosbackend.repository.LancamentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Testa DocumentoController.listar() isoladamente (sem Spring context, sem
 * BD real — repositórios mockados com Mockito), focado na lógica de
 * estadoDocumento() que combina Documento + Sugestao para calcular
 * Pendente/Analisado/Aprovado/Rejeitado. Peça 2 do desenvolvimento
 * incremental — segue [[DocumentoRepositoryTest]] (peça 1).
 */
class DocumentoControllerTest {

    private DocumentoRepository documentoRepository;
    private EntidadeRepository entidadeRepository;
    private SugestaoRepository sugestaoRepository;
    private LancamentoRepository lancamentoRepository;
    private DocumentoController controller;
    private User utilizador;

    @BeforeEach
    void setUp() {
        documentoRepository = Mockito.mock(DocumentoRepository.class);
        entidadeRepository = Mockito.mock(EntidadeRepository.class);
        sugestaoRepository = Mockito.mock(SugestaoRepository.class);
        lancamentoRepository = Mockito.mock(LancamentoRepository.class);
        controller = new DocumentoController(documentoRepository, entidadeRepository, sugestaoRepository, lancamentoRepository);

        utilizador = new User();
        utilizador.setId(1L);
    }

    @Test
    void listar_documentoSemSugestao_ficaPendente() {
        DocumentoContabilistico documento = documentoComId(10L, null);
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of())).thenReturn(List.of());
        when(sugestaoRepository.findAllByDocumentoId(10L)).thenReturn(List.of());

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).getEstado()).isEqualTo("Pendente");
        assertThat(resposta.get(0).getEntidadeNome()).isNull();
    }

    @Test
    void listar_sugestaoPendente_ficaAnalisado() {
        DocumentoContabilistico documento = documentoComId(11L, null);
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of())).thenReturn(List.of());
        when(sugestaoRepository.findAllByDocumentoId(11L))
                .thenReturn(List.of(sugestaoComEstado(EstadoSugestao.PENDENTE, LocalDateTime.now())));

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta.get(0).getEstado()).isEqualTo("Analisado");
    }

    @Test
    void listar_sugestaoAprovada_ficaAprovadoEComNomeDaEntidade() {
        Entidade entidade = new Entidade(5L, "Sonangol Distribuidora Lda", "5417002619", TipoEntidade.FORNECEDOR);
        DocumentoContabilistico documento = documentoComId(12L, 5L);
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of(5L))).thenReturn(List.of(entidade));
        when(sugestaoRepository.findAllByDocumentoId(12L))
                .thenReturn(List.of(sugestaoComEstado(EstadoSugestao.APROVADA, LocalDateTime.now())));

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta.get(0).getEstado()).isEqualTo("Aprovado");
        assertThat(resposta.get(0).getEntidadeNome()).isEqualTo("Sonangol Distribuidora Lda");
    }

    @Test
    void listar_sugestaoRejeitada_ficaRejeitado() {
        DocumentoContabilistico documento = documentoComId(13L, null);
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of())).thenReturn(List.of());
        when(sugestaoRepository.findAllByDocumentoId(13L))
                .thenReturn(List.of(sugestaoComEstado(EstadoSugestao.REJEITADA, LocalDateTime.now())));

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta.get(0).getEstado()).isEqualTo("Rejeitado");
    }

    @Test
    void listar_variasSugestoes_usaAEstadoDaMaisRecente() {
        DocumentoContabilistico documento = documentoComId(14L, null);
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of())).thenReturn(List.of());
        // Reanálise: a primeira sugestão foi rejeitada, a mais recente já foi aprovada.
        when(sugestaoRepository.findAllByDocumentoId(14L)).thenReturn(List.of(
                sugestaoComEstado(EstadoSugestao.REJEITADA, LocalDateTime.now().minusDays(1)),
                sugestaoComEstado(EstadoSugestao.APROVADA, LocalDateTime.now())
        ));

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta.get(0).getEstado()).isEqualTo("Aprovado");
    }

    @Test
    void listar_calculaTamanhoAPartirDosBytesDoConteudo() {
        DocumentoContabilistico documento = documentoComId(15L, null);
        documento.setConteudo(new byte[]{1, 2, 3, 4, 5});
        when(documentoRepository.findByUserId(1L)).thenReturn(List.of(documento));
        when(entidadeRepository.findAllById(List.of())).thenReturn(List.of());
        when(sugestaoRepository.findAllByDocumentoId(anyLong())).thenReturn(List.of());

        List<DocumentoResponseDTO> resposta = controller.listar(utilizador).getBody();

        assertThat(resposta.get(0).getTamanho()).isEqualTo(5);
    }

    private DocumentoContabilistico documentoComId(Long id, Long entidadeId) {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setId(id);
        documento.setNomeFicheiro("fatura.png");
        documento.setTipoConteudo("image/png");
        documento.setDataUpload(LocalDateTime.now());
        documento.setUserId(1L);
        documento.setEntidadeId(entidadeId);
        return documento;
    }

    private Sugestao sugestaoComEstado(EstadoSugestao estado, LocalDateTime dataCriacao) {
        Sugestao sugestao = new Sugestao();
        sugestao.setEstado(estado);
        sugestao.setDataCriacao(dataCriacao);
        return sugestao;
    }
}
