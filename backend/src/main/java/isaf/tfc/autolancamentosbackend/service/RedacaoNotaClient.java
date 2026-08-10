package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.GrupoEntidadeDTO;
import isaf.tfc.autolancamentosbackend.dto.NotaContaResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RedacaoNotaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fase 14 do plano de 20 fases — "Notas às Contas": pede ao FastAPI (POST
 * /notas/redacao, ver services/nota_redacao.py) um rascunho de texto
 * explicativo para uma nota JÁ CALCULADA (ver NotaContaService) — nunca
 * envia dados por calcular, só os valores finais já obtidos.
 *
 * Valores monetários são sempre enviados como String (nunca BigDecimal
 * bruto) — mesma convenção já usada em toda a fronteira Java↔FastAPI deste
 * projeto (ver LinhaSugeridaDTO, AnaliseResponse.valorTotal) para nunca
 * arriscar uma serialização numérica ambígua entre as duas linguagens.
 *
 * Best-effort, tal como PlanoContasClient/PerfilEntidadeClient: se o
 * FastAPI não responder, devolve null — quem chama decide como lidar com
 * a ausência de rascunho (nunca bloqueia a consulta da nota em si, que já
 * está disponível sem isto).
 */
@Service
public class RedacaoNotaClient {

    private static final Logger log = LoggerFactory.getLogger(RedacaoNotaClient.class);

    private final RestClient fastApiRestClient;

    public RedacaoNotaClient(RestClient fastApiRestClient) {
        this.fastApiRestClient = fastApiRestClient;
    }

    public RedacaoNotaDTO redigir(NotaContaResponseDTO nota) {
        try {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("conta", nota.getConta());
            corpo.put("nomeConta", nota.getNomeConta());
            corpo.put("natureza", nota.getNatureza());
            corpo.put("inicio", nota.getInicio() != null ? nota.getInicio().toString() : null);
            corpo.put("fim", nota.getFim() != null ? nota.getFim().toString() : null);
            corpo.put("totalDebito", formatar(nota.getTotalDebito()));
            corpo.put("totalCredito", formatar(nota.getTotalCredito()));
            corpo.put("saldo", formatar(nota.getSaldo()));
            corpo.put("porEntidade", grupos(nota.getPorEntidade()));

            return fastApiRestClient.post()
                    .uri("/notas/redacao")
                    .body(corpo)
                    .retrieve()
                    .body(RedacaoNotaDTO.class);
        } catch (Exception e) {
            log.warn("Não foi possível gerar a redação da nota (conta={}): {}", nota.getConta(), e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> grupos(List<GrupoEntidadeDTO> porEntidade) {
        if (porEntidade == null) {
            return List.of();
        }
        return porEntidade.stream().map(g -> {
            Map<String, Object> grupo = new LinkedHashMap<>();
            grupo.put("entidade", g.getEntidade());
            grupo.put("tipo", g.getTipo());
            grupo.put("totalDebito", formatar(g.getSubtotalDebito()));
            grupo.put("totalCredito", formatar(g.getSubtotalCredito()));
            return grupo;
        }).toList();
    }

    private String formatar(BigDecimal valor) {
        return (valor != null ? valor : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
