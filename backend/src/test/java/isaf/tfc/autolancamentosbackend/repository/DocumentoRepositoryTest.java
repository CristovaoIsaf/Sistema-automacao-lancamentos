package isaf.tfc.autolancamentosbackend.repository;

import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa DocumentoContabilistico + DocumentoRepository isoladamente, contra a
 * base de dados real configurada (não H2), porque a coluna "conteudo" na
 * tabela "documentos" é do tipo Postgres "oid" (Large Object), não "bytea" —
 * queremos confirmar que o mapeamento @Lob byte[] do Hibernate faz mesmo a
 * viagem de ida e volta sem perder/corromper os bytes.
 *
 * @Transactional faz rollback automático no fim de cada teste — não fica
 * lixo na base de dados de desenvolvimento.
 */
@SpringBootTest
@Transactional
class DocumentoRepositoryTest {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void guardaEDevolveDocumentoComConteudoBinarioIntacto() {
        byte[] conteudoOriginal = new byte[]{ (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4, 5 };

        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro("teste-repositorio.png");
        documento.setTipoConteudo("image/png");
        documento.setConteudo(conteudoOriginal);
        documento.setUserId(1L);

        DocumentoContabilistico guardado = documentoRepository.save(documento);
        Long id = guardado.getId();
        assertThat(id).isNotNull();

        // Força uma viagem real à BD — sem isto, o Hibernate podia devolver
        // o mesmo objeto em memória sem provar que os bytes sobrevivem à
        // gravação/leitura via Postgres.
        entityManager.flush();
        entityManager.clear();

        Optional<DocumentoContabilistico> encontradoOpt = documentoRepository.findById(id);
        assertThat(encontradoOpt).isPresent();

        DocumentoContabilistico encontrado = encontradoOpt.get();
        assertThat(encontrado.getNomeFicheiro()).isEqualTo("teste-repositorio.png");
        assertThat(encontrado.getTipoConteudo()).isEqualTo("image/png");
        assertThat(encontrado.getUserId()).isEqualTo(1L);
        assertThat(encontrado.getConteudo()).isEqualTo(conteudoOriginal);
        assertThat(encontrado.getDataUpload()).isNotNull();
        assertThat(encontrado.getEntidadeId()).isNull();
    }

    @Test
    void findByUserId_devolveApenasDocumentosDoUtilizadorPedido() {
        DocumentoContabilistico doUtilizador1 = new DocumentoContabilistico();
        doUtilizador1.setNomeFicheiro("utilizador1.png");
        doUtilizador1.setTipoConteudo("image/png");
        doUtilizador1.setConteudo(new byte[]{1});
        doUtilizador1.setUserId(101L);
        documentoRepository.save(doUtilizador1);

        DocumentoContabilistico doUtilizador2 = new DocumentoContabilistico();
        doUtilizador2.setNomeFicheiro("utilizador2.png");
        doUtilizador2.setTipoConteudo("image/png");
        doUtilizador2.setConteudo(new byte[]{2});
        doUtilizador2.setUserId(102L);
        documentoRepository.save(doUtilizador2);

        entityManager.flush();
        entityManager.clear();

        List<DocumentoContabilistico> resultado = documentoRepository.findByUserId(101L);

        assertThat(resultado)
                .hasSize(1)
                .allSatisfy(doc -> assertThat(doc.getUserId()).isEqualTo(101L));
    }
}
