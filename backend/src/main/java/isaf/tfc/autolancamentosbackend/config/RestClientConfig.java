package isaf.tfc.autolancamentosbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(@Value("${fastapi.services.url}") String baseUrl) {
        // Força HTTP/1.1: por omissão o HttpClient do JDK tenta um upgrade
        // cleartext para HTTP/2 (h2c) em pedidos POST com corpo em chunked
        // encoding. O uvicorn (FastAPI) não trata bem essa combinação e o
        // corpo multipart nunca chega à aplicação — o campo "ficheiro" surge
        // sempre como ausente, mesmo enviado corretamente.
        HttpClient jdkHttpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(jdkHttpClient))
                .build();
    }
}
