package isaf.tfc.autolancamentosbackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Condições para hospedar no Railway: a plataforma tem de conseguir
 * confirmar que o processo arrancou sem autenticação nenhuma (ver
 * SecurityConfig — antes desta correção, TODOS os endpoints exigiam JWT,
 * incluindo qualquer healthcheck). Prova, sem token nenhum, que
 * GET /health responde 200.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_semAutenticacao_devolveOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"ok\"}"));
    }
}
