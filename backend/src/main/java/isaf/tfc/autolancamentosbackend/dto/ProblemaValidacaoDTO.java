package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Um problema encontrado pelo motor de validação determinística (Fase 3 —
 * ver fastapi/app/services/document_validation.py::ProblemaValidacao).
 * Espelha exatamente o JSON devolvido por esse serviço.
 */
@Data
@NoArgsConstructor
public class ProblemaValidacaoDTO {

    private String campo;
    private String codigo;
    private String mensagem;

    // "erro" | "aviso" — ver document_validation.py::ResultadoValidacao.valido
    // (só "erro" invalida o documento; "aviso" é informativo).
    private String gravidade;
}
