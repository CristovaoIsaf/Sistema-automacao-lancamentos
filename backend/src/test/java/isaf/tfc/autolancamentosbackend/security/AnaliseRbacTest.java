package isaf.tfc.autolancamentosbackend.security;

import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria C02: antes desta correção, um Administrador conseguia
 * oficializar lançamentos analisando/aprovando/rejeitando sugestões da IA
 * (AnaliseController), apesar de "Administrador não faz lançamentos" já
 * estar aplicado no caminho manual (ver LancamentoController). Prova, com
 * pedidos HTTP reais (token JWT genuíno, utilizador persistido — não
 * mocks), que os três endpoints ficam agora reservados ao Contabilista.
 * Os pedidos usam ids/corpos inexistentes de propósito — @PreAuthorize
 * corre antes do corpo do método, por isso o 403 acontece sem tocar em
 * nenhuma Sugestao ou Documento real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnaliseRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void analisar_comTokenDeAdministrador_eBloqueadoComProibido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.ADMINISTRADOR, "admin-analise-rbac@exemplo.com");

        mockMvc.perform(post("/analises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentoId\":999999}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovar_comTokenDeAdministrador_eBloqueadoComProibido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.ADMINISTRADOR, "admin-aprovar-rbac@exemplo.com");

        mockMvc.perform(post("/analises/999999/aprovar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejeitar_comTokenDeAdministrador_eBloqueadoComProibido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.ADMINISTRADOR, "admin-rejeitar-rbac@exemplo.com");

        mockMvc.perform(post("/analises/999999/rejeitar")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void analisar_comTokenDeAuditor_eBloqueadoComProibido() throws Exception {
        String token = criarUtilizadorETokenComPapel(Role.AUDITOR, "auditor-analise-rbac@exemplo.com");

        mockMvc.perform(post("/analises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentoId\":999999}"))
                .andExpect(status().isForbidden());
    }

    private String criarUtilizadorETokenComPapel(Role papel, String email) {
        User user = new User();
        user.setNome("Utilizador Teste RBAC Analise");
        user.setEmail(email);
        user.setNif("6" + System.nanoTime() % 100000000L);
        user.setStatus("ATIVO");
        user.setSenha("$2a$10$placeholderHashNaoUsadoNesteTeste");
        user.setPapel(papel);
        userRepository.save(user);

        return jwtUtil.generateToken(email, papel);
    }
}
