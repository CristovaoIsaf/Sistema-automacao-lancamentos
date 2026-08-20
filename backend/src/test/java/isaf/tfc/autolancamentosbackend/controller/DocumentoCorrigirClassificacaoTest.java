package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.CorrigirClassificacaoRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.DocumentoResponseDTO;
import isaf.tfc.autolancamentosbackend.model.DocumentoContabilistico;
import isaf.tfc.autolancamentosbackend.model.EstadoSugestao;
import isaf.tfc.autolancamentosbackend.model.Sugestao;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.DocumentoRepository;
import isaf.tfc.autolancamentosbackend.repository.SugestaoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Auditoria C05: antes desta correção, PATCH /documentos/{id}/classificacao
 * sobrescrevia tipoDocumento/numeroDocumento/valor da Sugestao diretamente —
 * o que a IA tinha sugerido originalmente desaparecia, sem nenhum rasto.
 * Confirma, com dados reais persistidos na BD (não mocks), que a primeira
 * correção guarda um snapshot em *OriginalIA e que correções seguintes não
 * o voltam a sobrescrever. Chama o controller diretamente (mesmo padrão de
 * DocumentoControllerIntegrationTest); como corrigirClassificacao tem
 * @PreAuthorize, o SecurityContext tem de ser preenchido manualmente — não
 * há filtro JWT nesta invocação direta.
 */
@SpringBootTest
@Transactional
class DocumentoCorrigirClassificacaoTest {

    @Autowired
    private DocumentoController documentoController;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private SugestaoRepository sugestaoRepository;

    @Autowired
    private EntityManager entityManager;

    private final User contabilista = new User();

    @BeforeEach
    void autenticarComoContabilista() {
        contabilista.setId(42L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        contabilista, null, List.of(new SimpleGrantedAuthority("ROLE_CONTABILISTA"))));
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    private Long criarDocumentoComSugestao(String tipoDocumento, String numeroDocumento, String valor) {
        DocumentoContabilistico documento = new DocumentoContabilistico();
        documento.setNomeFicheiro("fatura-c05.png");
        documento.setTipoConteudo("image/png");
        documento.setConteudo(new byte[]{1, 2, 3});
        documento.setUserId(1L);
        documento = documentoRepository.save(documento);

        Sugestao sugestao = new Sugestao();
        sugestao.setDocumentoId(documento.getId());
        sugestao.setTipoDocumento(tipoDocumento);
        sugestao.setNumeroDocumento(numeroDocumento);
        sugestao.setValor(valor);
        sugestao.setEstado(EstadoSugestao.PENDENTE);
        sugestao.setDataCriacao(LocalDateTime.now());
        sugestaoRepository.save(sugestao);

        entityManager.flush();
        entityManager.clear();
        return documento.getId();
    }

    @Test
    void primeiraCorrecao_preservaOValorOriginalDaIA() {
        Long documentoId = criarDocumentoComSugestao("a_classificar", "FT 2026/001", "1.000,00 Kz");

        CorrigirClassificacaoRequestDTO pedido = new CorrigirClassificacaoRequestDTO();
        pedido.setTipoDocumento("fatura_recibo");

        DocumentoResponseDTO resposta = documentoController
                .corrigirClassificacao(documentoId, pedido, contabilista).getBody();

        assertThat(resposta.getTipoDocumento()).isEqualTo("fatura_recibo");
        assertThat(resposta.getTipoDocumentoOriginalIA()).isEqualTo("a_classificar");
        // Campos não corrigidos nesta chamada continuam preservados nos dois lados.
        assertThat(resposta.getNumeroDocumento()).isEqualTo("FT 2026/001");
        assertThat(resposta.getNumeroDocumentoOriginalIA()).isEqualTo("FT 2026/001");
        assertThat(resposta.getCorrigidoEm()).isNotNull();
    }

    @Test
    void segundaCorrecao_naoSobrescreveOSnapshotOriginalJaGuardado() {
        Long documentoId = criarDocumentoComSugestao("a_classificar", "FT 2026/001", "1.000,00 Kz");

        CorrigirClassificacaoRequestDTO primeiraCorrecao = new CorrigirClassificacaoRequestDTO();
        primeiraCorrecao.setTipoDocumento("fatura_recibo");
        documentoController.corrigirClassificacao(documentoId, primeiraCorrecao, contabilista);

        CorrigirClassificacaoRequestDTO segundaCorrecao = new CorrigirClassificacaoRequestDTO();
        segundaCorrecao.setTipoDocumento("nota_credito");
        DocumentoResponseDTO resposta = documentoController
                .corrigirClassificacao(documentoId, segundaCorrecao, contabilista).getBody();

        assertThat(resposta.getTipoDocumento()).isEqualTo("nota_credito");
        // O snapshot continua a refletir a sugestão ORIGINAL da IA, não a
        // primeira correção.
        assertThat(resposta.getTipoDocumentoOriginalIA()).isEqualTo("a_classificar");
    }

    @Test
    void semNenhumCampoAlterado_naoMarcaComoCorrigidoNemCriaSnapshot() {
        Long documentoId = criarDocumentoComSugestao("a_classificar", "FT 2026/001", "1.000,00 Kz");

        DocumentoResponseDTO resposta = documentoController
                .corrigirClassificacao(documentoId, new CorrigirClassificacaoRequestDTO(), contabilista).getBody();

        assertThat(resposta.getTipoDocumentoOriginalIA()).isNull();
        assertThat(resposta.getCorrigidoEm()).isNull();
    }
}
