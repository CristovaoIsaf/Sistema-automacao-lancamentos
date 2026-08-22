package isaf.tfc.autolancamentosbackend.controller;

import com.jayway.jsonpath.JsonPath;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import isaf.tfc.autolancamentosbackend.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo de 2FA através de pedidos HTTP reais (token JWT genuíno,
 * utilizador persistido de verdade, código TOTP calculado com a mesma
 * biblioteca usada em produção) — mesmo padrão de DocumentoRbacTest/
 * EstadoUtilizadorRbacTest. Cobre o percurso ponta-a-ponta: setup →
 * confirmar (código errado depois certo) → status → desativar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TwoFactorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String criarUtilizadorETokenComPassword(String password) {
        User user = new User();
        user.setNome("Utilizador Teste 2FA");
        user.setEmail("duplo-fator-" + System.nanoTime() + "@exemplo.com");
        user.setNif("6" + System.nanoTime() % 100000000L);
        user.setStatus("ATIVO");
        user.setSenha(passwordEncoder.encode(password));
        user.setPapel(Role.CONTABILISTA);
        user = userRepository.save(user);

        return jwtUtil.generateToken(user.getEmail(), user.getPapel());
    }

    @Test
    void fluxoCompleto_setupConfirmarStatusDesativar() throws Exception {
        String token = criarUtilizadorETokenComPassword("senhaDaConta123");

        // Estado inicial: nunca ativo.
        mockMvc.perform(get("/api/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // Passo 1: iniciar setup.
        String respostaSetup = mockMvc.perform(post("/api/2fa/setup").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpauthUrl").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(respostaSetup, "$.secret");

        // Confirmar com código errado — não activa.
        mockMvc.perform(post("/api/2fa/confirmar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"000000\"}"))
                .andExpect(status().is5xxServerError());

        mockMvc.perform(get("/api/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.ativo").value(false));

        // Confirmar com o código real (calculado com a mesma biblioteca de produção).
        String codigoValido = new DefaultCodeGenerator().generate(secret, new SystemTimeProvider().getTime() / 30);
        String respostaConfirmar = mockMvc.perform(post("/api/2fa/confirmar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigoValido + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<String> codigosRecuperacao = JsonPath.read(respostaConfirmar, "$.codigosRecuperacao");
        assertThat(codigosRecuperacao).hasSize(8);

        mockMvc.perform(get("/api/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.ativo").value(true));

        // Desativar com a password errada — continua ativo.
        mockMvc.perform(post("/api/2fa/desativar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password-errada\"}"))
                .andExpect(status().is5xxServerError());

        mockMvc.perform(get("/api/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.ativo").value(true));

        // Desativar com a password certa — desativa mesmo.
        mockMvc.perform(post("/api/2fa/desativar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"senhaDaConta123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/2fa/status").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void login_comDoisFactoresAtivos_exigeCodigoAntesDeEmitirToken() throws Exception {
        String token = criarUtilizadorETokenComPassword("senhaDaConta123");
        User user = userRepository.findAll().stream()
                .filter(u -> jwtUtil.extractEmail(token).equals(u.getEmail()))
                .findFirst().orElseThrow();

        String respostaSetup = mockMvc.perform(post("/api/2fa/setup").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        String secret = JsonPath.read(respostaSetup, "$.secret");
        String codigoValido = new DefaultCodeGenerator().generate(secret, new SystemTimeProvider().getTime() / 30);
        mockMvc.perform(post("/api/2fa/confirmar")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigoValido + "\"}"))
                .andExpect(status().isOk());

        // Login só com email+password: não emite token, pede o 2º passo.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"senhaDaConta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTwoFactor").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());

        // Login com email+password+código: emite token normalmente.
        String codigoLogin = new DefaultCodeGenerator().generate(secret, new SystemTimeProvider().getTime() / 30);
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"senhaDaConta123\",\"codigo2FA\":\"" + codigoLogin + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresTwoFactor").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
