package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.AnaliseResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementação de AnalisadorDocumentoIA que fala com o serviço FastAPI real
 * (POST /analisar), enviando o ficheiro por multipart tal como o endpoint espera.
 *
 * A causa raiz de um bug em que o FastAPI recebia sempre o campo "ficheiro"
 * como ausente estava no RestClientConfig (upgrade cleartext para HTTP/2 mal
 * suportado pelo uvicorn) — ver o comentário lá. Não é preciso mexer aqui
 * para isso; o HttpEntity só garante o Content-Type correto da parte.
 */
@Service
public class FastApiAnaliseClient implements AnalisadorDocumentoIA {

    private final RestClient fastApiRestClient;

    public FastApiAnaliseClient(RestClient fastApiRestClient) {
        this.fastApiRestClient = fastApiRestClient;
    }

    @Override
    public AnaliseResponse analisar(byte[] conteudo, String nomeFicheiro, String fingerprint) {
        ByteArrayResource resource = new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nomeFicheiro;
            }
        };

        HttpHeaders ficheiroHeaders = new HttpHeaders();
        ficheiroHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("ficheiro", new HttpEntity<>(resource, ficheiroHeaders));
        // Fase 4 — ver AnalisadorDocumentoIA.analisar: reenvia o hash já
        // calculado no upload em vez de deixar o FastAPI recalculá-lo.
        if (fingerprint != null && !fingerprint.isBlank()) {
            body.add("fingerprint", fingerprint);
        }

        try {
            return fastApiRestClient.post()
                    .uri("/analisar")
                    .body(body)
                    .retrieve()
                    .body(AnaliseResponse.class);

        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Erro ao chamar a API de análise: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException("Não foi possível comunicar com a API de análise: " + e.getMessage(), e);
        }
    }
}
