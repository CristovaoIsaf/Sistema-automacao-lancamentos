package isaf.tfc.autolancamentosbackend.security;

import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Peça 3 do bloco "Autenticação/utilizadores": prova, com um pedido HTTP
 * real (token JWT genuíno, utilizador persistido de verdade — não mocks),
 * que RN010 ("escrita de documentos fica fora do Auditor") não está a ser
 * aplicada no servidor. POST /documentos/upload é usado por ser a escrita
 * mais simples do fluxo (sem chamadas externas ao FastAPI).
 *
 * Antes da correção (@PreAuthorize em DocumentoController.upload): um
 * Auditor consegue chamar isto e recebe 200 — é o que este teste confirma
 * primeiro (o "vermelho"). Depois da correção, passa a esperar 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentoRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void upload_comTokenDeAuditor_eBloqueadoComProibido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.AUDITOR, "auditor-rbac-teste@exemplo.com");

        MockMultipartFile ficheiro = new MockMultipartFile(
                "file", "teste.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/documentos")
                        .file(ficheiro)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_comTokenDeContabilista_ePermitido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.CONTABILISTA, "contabilista-rbac-teste@exemplo.com");

        MockMultipartFile ficheiro = new MockMultipartFile(
                "file", "teste.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/documentos")
                        .file(ficheiro)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String criarUtilizadorETokenComPapel(Role papel, String email) {
        User user = new User();
        user.setNome("Utilizador Teste RBAC");
        user.setEmail(email);
        user.setNif("5" + System.nanoTime() % 100000000L);
        user.setStatus("ATIVO");
        user.setSenha("$2a$10$placeholderHashNaoUsadoNesteTeste");
        user.setPapel(papel);
        userRepository.save(user);

        return jwtUtil.generateToken(email, papel);
    }
}
