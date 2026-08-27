package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.PerfilEntidadeDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Peça "PerfilEntidadeClient" do desenvolvimento incremental — testa
 * isoladamente (MockRestServiceServer) o comportamento "best-effort": nunca
 * bloqueia o dossiê da entidade por esta parte opcional (ver Javadoc da
 * classe).
 */
class PerfilEntidadeClientTest {

    private RestClient.Builder builder() {
        return RestClient.builder().baseUrl("http://fastapi.test");
    }

    @Test
    void obter_nifNulo_devolveNullSemChamarOFastApi() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // Nenhuma expectativa registada -- se o cliente chamasse o FastAPI
        // mesmo assim, o MockRestServiceServer falhava por pedido inesperado.

        PerfilEntidadeClient client = new PerfilEntidadeClient(builder.build());

        assertThat(client.obter(null)).isNull();
        server.verify();
    }

    @Test
    void obter_nifEmBranco_devolveNullSemChamarOFastApi() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        PerfilEntidadeClient client = new PerfilEntidadeClient(builder.build());

        assertThat(client.obter("   ")).isNull();
        server.verify();
    }

    @Test
    void obter_fastApiDisponivel_devolveOPerfil() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/perfil-entidade/5417002619"))
                .andRespond(withSuccess(
                        "{\"nif\":\"5417002619\",\"totalDocumentos\":3,\"tipoDominante\":\"compra_mercadoria\",\"distribuicaoTiposDocumento\":{\"compra_mercadoria\":3}}",
                        MediaType.APPLICATION_JSON));

        PerfilEntidadeClient client = new PerfilEntidadeClient(builder.build());
        PerfilEntidadeDTO perfil = client.obter("5417002619");

        assertThat(perfil.getNif()).isEqualTo("5417002619");
        assertThat(perfil.getTotalDocumentos()).isEqualTo(3);
        assertThat(perfil.getTipoDominante()).isEqualTo("compra_mercadoria");
    }

    @Test
    void obter_fastApiIndisponivel_devolveNullEmVezDeLancar() {
        RestClient.Builder builder = builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://fastapi.test/perfil-entidade/5417002619"))
                .andRespond(withServerError());

        PerfilEntidadeClient client = new PerfilEntidadeClient(builder.build());

        assertThat(client.obter("5417002619")).isNull();
    }
}
