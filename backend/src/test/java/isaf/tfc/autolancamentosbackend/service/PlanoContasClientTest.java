package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import org.junit.jupiter.api.Test;
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
 * Peça "PlanoContasClient" do desenvolvimento incremental — testa
 * isoladamente (MockRestServiceServer, sem FastAPI real nem Spring context)
 * o cache em memória e o comportamento "best-effort" (nunca rebenta o
 * arranque se o FastAPI estiver em baixo — ver Javadoc da classe).
 */
class PlanoContasClientTest {

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("http://fastapi.test");
    }

    @Test
    void carregarNoArranque_fastApiDisponivel_preencheOCache() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/pgc/contas"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"codigo\":\"31\",\"nome\":\"Clientes\",\"classe\":\"3\",\"subconta\":null,\"natureza\":\"DEVEDORA\"}]",
                        MediaType.APPLICATION_JSON));

        PlanoContasClient client = new PlanoContasClient(builder.build());
        client.carregarNoArranque();

        assertThat(client.listar()).hasSize(1);
        assertThat(client.listar().get(0).getCodigo()).isEqualTo("31");
        server.verify();
    }

    @Test
    void carregarNoArranque_fastApiIndisponivel_naoLancaExcecaoEFicaComCacheVazio() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // manyTimes(): com o cache ainda vazio depois de uma falha, listar()
        // tenta de novo lazily (ver Javadoc de PlanoContasClient/
        // tentarCarregar) — este teste chama carregarNoArranque() e depois
        // listar(), por isso o FastAPI é mesmo chamado duas vezes.
        server.expect(org.springframework.test.web.client.ExpectedCount.manyTimes(), requestTo("http://fastapi.test/pgc/contas"))
                .andRespond(withServerError());

        PlanoContasClient client = new PlanoContasClient(builder.build());

        // Best-effort: nunca lança, mesmo com o FastAPI a devolver 500.
        client.carregarNoArranque();

        assertThat(client.listar()).isEmpty();
    }

    @Test
    void listar_comCacheJaPreenchido_naoFazSegundoPedidoHttp() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // Só UMA expectativa registada — se listar() disparasse um segundo
        // pedido, o MockRestServiceServer falhava por "no further requests expected".
        server.expect(requestTo("http://fastapi.test/pgc/contas"))
                .andRespond(withSuccess("[{\"codigo\":\"31\",\"nome\":\"Clientes\"}]", MediaType.APPLICATION_JSON));

        PlanoContasClient client = new PlanoContasClient(builder.build());
        client.carregarNoArranque();

        client.listar();
        client.listar();
        client.listar();

        server.verify();
    }

    @Test
    void porCodigo_codigoExistente_devolveAConta() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/pgc/contas"))
                .andRespond(withSuccess("[{\"codigo\":\"31\",\"nome\":\"Clientes\"}]", MediaType.APPLICATION_JSON));

        PlanoContasClient client = new PlanoContasClient(builder.build());
        client.carregarNoArranque();

        ContaDTO conta = client.porCodigo("31");

        assertThat(conta.getNome()).isEqualTo("Clientes");
    }

    @Test
    void porCodigo_codigoInexistente_lancaIllegalStateException() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/pgc/contas"))
                .andRespond(withSuccess("[{\"codigo\":\"31\",\"nome\":\"Clientes\"}]", MediaType.APPLICATION_JSON));

        PlanoContasClient client = new PlanoContasClient(builder.build());
        client.carregarNoArranque();

        assertThatThrownBy(() -> client.porCodigo("99.99"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("99.99");
    }
}
