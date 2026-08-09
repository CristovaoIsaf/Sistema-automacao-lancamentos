package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Uma opção de resposta à pergunta de contextualização (Fase 4 do plano de
 * 20 fases — ver document_analyzer.py::_construir_pergunta_contextualizacao).
 * "linhas" já vem pré-calculado pelo FastAPI (pgc_ao.construir_lancamento)
 * para o frontend poder aplicar a escolha instantaneamente, sem novo
 * pedido ao FastAPI.
 */
@Data
@NoArgsConstructor
public class OpcaoContextualizacaoDTO {

    private String valor;
    private String rotulo;
    private String tipo;
    private String categoria;
    private List<LinhaSugeridaDTO> linhas;
}
