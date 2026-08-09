package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Fase 6 do plano de 20 fases: plano de contas — única fonte de verdade.
 *
 * Antes desta fase, ContaController mantinha uma cópia manual, hardcoded,
 * das mesmas contas já definidas em fastapi/app/services/pgc.py — duas
 * listas que tinham de ser sincronizadas à mão a cada alteração. Este
 * cliente busca o plano de contas real a GET /pgc/contas (FastAPI) e
 * cacheia-o em memória; nenhuma conta é redefinida aqui.
 *
 * Busca ao arranque (best-effort — se o FastAPI ainda não estiver de pé
 * nesse momento, não impede o Spring Boot de arrancar, ver tentarCarregar)
 * e, em caso de cache ainda vazio, tenta de novo lazily no primeiro pedido.
 */
@Service
public class PlanoContasClient {

    private static final Logger log = LoggerFactory.getLogger(PlanoContasClient.class);

    private final RestClient fastApiRestClient;
    private volatile List<ContaDTO> contas = List.of();

    public PlanoContasClient(RestClient fastApiRestClient) {
        this.fastApiRestClient = fastApiRestClient;
    }

    @PostConstruct
    void carregarNoArranque() {
        tentarCarregar();
    }

    public List<ContaDTO> listar() {
        if (contas.isEmpty()) {
            tentarCarregar();
        }
        return contas;
    }

    /**
     * Devolve a conta com o código indicado. Lança se o código não existir
     * no plano de contas — falha alto e cedo: uma categoria/lançamento a
     * referenciar um código inexistente é um erro de configuração, não um
     * caso a tratar em runtime.
     */
    public ContaDTO porCodigo(String codigo) {
        return listar().stream()
                .filter(c -> c.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Código de conta '" + codigo + "' não existe no plano de contas (GET /pgc/contas)."));
    }

    private synchronized void tentarCarregar() {
        if (!contas.isEmpty()) {
            return;
        }
        try {
            List<ContaDTO> resultado = fastApiRestClient.get()
                    .uri("/pgc/contas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ContaDTO>>() {
                    });
            if (resultado != null && !resultado.isEmpty()) {
                contas = resultado;
            }
        } catch (Exception e) {
            log.warn("Não foi possível carregar o plano de contas a partir do FastAPI (GET /pgc/contas): {}", e.getMessage());
        }
    }
}
