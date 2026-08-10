package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Fase 12 do plano de 20 fases ("Perfil de Entidade" — "reutilizar
 * conhecimento"): espelha o shape devolvido por GET /perfil-entidade/{nif}
 * (FastAPI, ver services/entity_profile.py `resumo()`), o mesmo
 * conhecimento já usado internamente para poupar chamadas de IA
 * (document_analyzer.py._classificar_por_perfil_entidade), agora exposto
 * também no dossiê da entidade (ver EntidadeController).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PerfilEntidadeDTO {

    private String nif;

    private int totalDocumentos;

    // null quando o histórico ainda não é suficiente/unânime — ver
    // entity_profile.tipo_dominante. Nunca "adivinhado" no lado Java.
    private String tipoDominante;

    // "documentos habituais": quantas vezes cada tipo de documento já foi
    // visto para esta entidade — útil mesmo sem tipoDominante definido.
    private Map<String, Integer> distribuicaoTiposDocumento;
}
