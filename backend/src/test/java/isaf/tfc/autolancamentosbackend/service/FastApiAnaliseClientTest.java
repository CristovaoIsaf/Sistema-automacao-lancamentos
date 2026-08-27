package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Peça "FastApiAnaliseClient" do desenvolvimento incremental — testa
 * isoladamente (MockRestServiceServer, sem FastAPI real) que o pedido é
 * enviado ao endpoint certo e que a resposta/erro do FastAPI são tratados
 * corretamente. Não valida o conteúdo exacto do corpo multipart (frágil de
 * afirmar bit-a-bit e já coberto ao vivo — ver histórico desta sessão, o
 * bug real de "campo ficheiro sempre ausente" foi encontrado e corrigido
 * com testes HTTP reais contra o FastAPI, não com uma asserção de mock).
 */
class FastApiAnaliseClientTest {

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("http://fastapi.test");
    }

    @Test
    void analisar_fastApiDisponivel_devolveAResposta() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/analisar"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"success\":true,\"tipoDocumento\":\"compra_mercadoria\",\"confianca\":80,\"linhas\":[]}",
                        MediaType.APPLICATION_JSON));

        FastApiAnaliseClient client = new FastApiAnaliseClient(builder.build());
        AnaliseResponse resposta = client.analisar(new byte[]{1, 2, 3}, "fatura.png", "hash-abc", null);

        assertThat(resposta.isSuccess()).isTrue();
        assertThat(resposta.getTipoDocumento()).isEqualTo("compra_mercadoria");
        assertThat(resposta.getConfianca()).isEqualTo(80);
    }

    @Test
    void analisar_fastApiDevolveErro_lancaRuntimeExceptionComOEstadoHttp() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/analisar"))
                .andRespond(withServerError().body("Erro interno do FastAPI"));

        FastApiAnaliseClient client = new FastApiAnaliseClient(builder.build());

        assertThatThrownBy(() -> client.analisar(new byte[]{1, 2, 3}, "fatura.png", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("500");
    }

    @Test
    void analisar_fastApiInacessivel_lancaRuntimeExceptionSemRebentarComOutroTipo() {
        // URL real mas sem nada a ouvir na porta -- gera uma falha de
        // ligação genuína (ConnectException), não uma resposta HTTP com
        // estado de erro, para exercitar o catch(Exception) genérico em vez
        // do catch(RestClientResponseException) (já coberto no teste
        // acima). Não usa MockRestServiceServer aqui de propósito: sem
        // nenhuma expectativa registada, ele próprio lança AssertionError
        // (um Error, não uma Exception) para assinalar "pedido inesperado"
        // -- não é o mesmo tipo de falha que este teste quer simular, e o
        // catch(Exception) do próprio FastApiAnaliseClient nunca o apanharia.
        RestClient clienteInalcancavel = RestClient.builder().baseUrl("http://localhost:1").build();
        FastApiAnaliseClient client = new FastApiAnaliseClient(clienteInalcancavel);

        assertThatThrownBy(() -> client.analisar(new byte[]{1, 2, 3}, "fatura.png", null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não foi possível comunicar com a API de análise");
    }
}
