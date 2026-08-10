package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.PerfilEntidadeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Fase 12 do plano de 20 fases — "Perfil de Entidade": busca ao FastAPI
 * (GET /perfil-entidade/{nif}, ver services/entity_profile.py) o
 * conhecimento já acumulado sobre uma entidade, para anexar ao dossiê
 * (ver EntidadeController). Chamada só quando o dossiê é consultado — não
 * está no caminho quente da análise de documentos, ao contrário de
 * PlanoContasClient (por isso não precisa de cache: o volume de consultas
 * ao dossiê de uma entidade é baixo).
 *
 * Best-effort, tal como PlanoContasClient: se o FastAPI não responder, o
 * dossiê continua a devolver os documentos da entidade normalmente, só
 * sem o perfil (nunca falha o pedido inteiro por esta parte opcional).
 *
 * NOTA DE ÂMBITO — o NIF do perfil nem sempre é o NIF "da entidade" do
 * dossiê: entity_profile.py regista sempre pelo NIF de quem EMITIU o
 * documento (dados_fatura.emitente_nif), independentemente do tipo de
 * operação — é essa a pergunta que o gate responde ("documentos emitidos
 * por X costumam ser classificados como Z?"), decidida ANTES de se saber
 * o tipo. Já o NIF da Entidade do dossiê (ver
 * DocumentAnalyzer._entidade_e_nif no FastAPI) é o do EMITENTE numa
 * compra mas o do ADQUIRENTE numa venda/prestação/recebimento. Coincidem
 * sempre no exemplo típico desta fase (perfil de FORNECEDOR — compras),
 * mas para uma entidade CLIENTE (vendas) este método pode devolver null
 * mesmo que exista perfil sob outro NIF — nunca dados errados (o
 * `resumo()` do FastAPI nunca inventa), só ausência de dados nesse caso.
 * Unificar os dois conceitos de "entidade" ficaria fora do âmbito desta
 * fase (mudaria semântica já usada noutras fases, não só aditiva).
 */
@Service
public class PerfilEntidadeClient {

    private static final Logger log = LoggerFactory.getLogger(PerfilEntidadeClient.class);

    private final RestClient fastApiRestClient;

    public PerfilEntidadeClient(RestClient fastApiRestClient) {
        this.fastApiRestClient = fastApiRestClient;
    }

    public PerfilEntidadeDTO obter(String nif) {
        if (nif == null || nif.isBlank()) {
            return null;
        }
        try {
            // {nif} como variável de template — o RestClient trata a
            // codificação percent-encoding automaticamente; codificar aqui
            // manualmente duplicaria a codificação.
            return fastApiRestClient.get()
                    .uri("/perfil-entidade/{nif}", nif)
                    .retrieve()
                    .body(PerfilEntidadeDTO.class);
        } catch (Exception e) {
            log.warn("Não foi possível obter o perfil da entidade (NIF={}) a partir do FastAPI: {}", nif, e.getMessage());
            return null;
        }
    }
}
