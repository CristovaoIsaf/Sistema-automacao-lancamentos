package isaf.tfc.autolancamentosbackend.dto;

import lombok.Data;

/**
 * Corpo de PATCH /documentos/{id}/classificacao — corrige campos da
 * Sugestao mais recente ligada ao documento, quando a IA/regras
 * classificaram mal (RN010 aplicada como no resto do fluxo: só
 * Administrador/Contabilista, nunca Auditor). Campos nulos ficam
 * inalterados — permite corrigir só um dos três sem reenviar os outros.
 */
@Data
public class CorrigirClassificacaoRequestDTO {

    private String tipoDocumento;

    private String numeroDocumento;

    private String valor;

}
