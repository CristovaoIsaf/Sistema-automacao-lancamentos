package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Espelha exatamente o JSON devolvido por POST /analisar do serviço FastAPI
 * (ver fastapi/app/services/document_analyzer.py::_montar_resposta).
 */
@Data
@NoArgsConstructor
public class AnaliseResponse {

    private boolean success;
    private String tipoDocumento;
    private String descricao;
    private String valorTotal;
    private String moeda;
    private String entidade;
    private String nif;
    private String data;
    private Integer confianca;
    private String modelo;
    private List<LinhaSugeridaDTO> linhas;
    private String textoOcr;
    private Double confiancaOcr;

    // Trechos do Decreto 82/01 usados pelo RAG para fundamentar a classificação
    // (vazio quando a classificação veio do fallback por regras).
    private String fundamentacao;

    // Categoria do plano de contas (ver CategoriaContaController) inferida
    // a partir do tipoDocumento — ver pgc_ao.categoria_do_tipo no FastAPI.
    private String categoria;
}
