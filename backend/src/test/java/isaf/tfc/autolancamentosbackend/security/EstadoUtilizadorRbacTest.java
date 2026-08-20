package isaf.tfc.autolancamentosbackend.security;

import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria C09: até aqui, um token JWT continuava válido até expirar (10
 * dias — ver application.properties jwt.expiration) mesmo que o
 * Administrador suspendesse o utilizador entretanto — JwtAuthFilter nunca
 * verificava o estado atual do utilizador na BD, só a assinatura/validade
 * do próprio token. Prova, com um token JWT genuíno e um utilizador
 * persistido de verdade (não mocks), que suspender a conta (status
 * "INATIVO") bloqueia pedidos já autenticados com esse token, não só
 * logins novos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EstadoUtilizadorRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void tokenDeUtilizadorSuspensoDepoisDeEmitido_deixaDeAutenticarPedidos() throws Exception {
        User user = new User();
        user.setNome("Utilizador Teste Estado");
        user.setEmail("estado-rbac-teste@exemplo.com");
        user.setNif("7" + System.nanoTime() % 100000000L);
        user.setStatus("ATIVO");
        user.setSenha("$2a$10$placeholderHashNaoUsadoNesteTeste");
        user.setPapel(Role.CONTABILISTA);
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getPapel());

        // Ainda ATIVO: o token autentica normalmente.
        mockMvc.perform(get("/api/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Administrador suspende a conta — mesmo token, sem novo login.
        user.setStatus("INATIVO");
        userRepository.save(user);

        mockMvc.perform(get("/api/lancamentos").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
