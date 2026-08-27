package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RedacaoNotaDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Peça "RedacaoNotaClient" do desenvolvimento incremental — testa
 * isoladamente (MockRestServiceServer) que os valores monetários vão sempre
 * como String formatada (nunca BigDecimal bruto, ver Javadoc da classe) e o
 * comportamento "best-effort" (devolve null em vez de rebentar).
 */
class RedacaoNotaClientTest {

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("http://fastapi.test");
    }

    private NotaContaResponseDTO nota() {
        return new NotaContaResponseDTO(
                "32", "Fornecedores", "CREDORA",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                java.util.List.of(),
                new BigDecimal("1000"), new BigDecimal("2500.5"), new BigDecimal("-1500.5"));
    }

    @Test
    void redigir_fastApiDisponivel_devolveORascunho() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/notas/redacao"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Valores monetários sempre como texto com 2 casas decimais.
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalDebito\":\"1000.00\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalCredito\":\"2500.50\"")))
                .andRespond(withSuccess("{\"texto\":\"Rascunho gerado.\",\"fonte\":\"ia\"}", MediaType.APPLICATION_JSON));

        RedacaoNotaClient client = new RedacaoNotaClient(builder.build());
        RedacaoNotaDTO resultado = client.redigir(nota());

        assertThat(resultado.getTexto()).isEqualTo("Rascunho gerado.");
        assertThat(resultado.getFonte()).isEqualTo("ia");
    }

    @Test
    void redigir_fastApiIndisponivel_devolveNullEmVezDeLancar() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/notas/redacao"))
                .andRespond(withServerError());

        RedacaoNotaClient client = new RedacaoNotaClient(builder.build());

        assertThat(client.redigir(nota())).isNull();
    }
}
