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
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração real da peça 1 (DocumentoContabilistico/DocumentoRepository —
 * ver DocumentoRepositoryTest) com a peça 2 (lógica de estado em
 * DocumentoController.listar() — ver DocumentoControllerTest): persiste
 * documento + entidade + sugestão de verdade na BD configurada e confirma
 * que o endpoint devolve o estado/entidade corretos a partir de dados reais,
 * não mocks. @Transactional faz rollback no fim — não fica lixo na BD.
 */
@SpringBootTest
@Transactional
class DocumentoControllerIntegrationTest {

    @Autowired
    private DocumentoController documentoController;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private EntidadeRepository entidadeRepository;

    @Autowired
    private SugestaoRepository sugestaoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void listar_documentoRealAprovadoComEntidadeReal_devolveEstadoENomeCorretos() {
        Entidade entidade = new Entidade();
        entidade.setNome("Sonangol Distribuidora Lda");
        entidade.setNif("5417002619");
        entidade.setTipo(TipoEntidade.FORNECEDOR);
        entidade = entidadeRepository.save(entidade);

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro("fatura-integracao.png");
        documento.setTipoConteudo("image/png");
        documento.setConteudo(new byte[]{10, 20, 30});
        documento.setUserId(999L);
        documento.setEntidadeId(entidade.getId());
        documento = documentoRepository.save(documento);

        Sugestao sugestao = new Sugestao();
        sugestao.setDocumentoId(documento.getId());
        sugestao.setEstado(EstadoSugestao.APROVADA);
        sugestao.setDataCriacao(LocalDateTime.now());
        sugestaoRepository.save(sugestao);

        entityManager.flush();
        entityManager.clear();

        User utilizador = new User();
        utilizador.setId(999L);

        Long documentoId = documento.getId();
        List<DocumentoResponseDTO> resposta = documentoController.listar(utilizador).getBody();

        DocumentoResponseDTO dto = resposta.stream()
                .filter(d -> d.getId().equals(documentoId))
                .findFirst()
                .orElseThrow();

        assertThat(dto.getEstado()).isEqualTo("Aprovado");
        assertThat(dto.getEntidadeNome()).isEqualTo("Sonangol Distribuidora Lda");
        assertThat(dto.getTamanho()).isEqualTo(3);
    }

    @Test
    void listar_documentoRealSemSugestao_ficaPendente() {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro("fatura-pendente.png");
        documento.setTipoConteudo("image/png");
        documento.setConteudo(new byte[]{1});
        documento.setUserId(998L);
        documento = documentoRepository.save(documento);

        entityManager.flush();
        entityManager.clear();

        User utilizador = new User();
        utilizador.setId(998L);

        List<DocumentoResponseDTO> resposta = documentoController.listar(utilizador).getBody();

        assertThat(resposta).hasSize(1);
        assertThat(resposta.get(0).getEstado()).isEqualTo("Pendente");
        assertThat(resposta.get(0).getEntidadeNome()).isNull();
    }
}
