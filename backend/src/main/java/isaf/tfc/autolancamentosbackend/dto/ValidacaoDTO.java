package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado do motor de validação determinística (Fase 3 do mapa de
 * impacto — ver fastapi/app/services/document_validation.py). Espelha
 * exatamente o JSON devolvido por POST /analisar (chave "validacao").
 */
@Data
@NoArgsConstructor
public class ValidacaoDTO {

    private boolean valido;
    private String versao;
    private List<ProblemaValidacaoDTO> problemas;
}
