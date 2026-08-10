package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fase 14 do plano de 20 fases — "Notas às Contas": espelha o shape
 * devolvido por POST /notas/redacao (FastAPI, ver services/nota_redacao.py).
 * `fonte` é "ia" ou "template" — nunca escondida do frontend, para o
 * contabilista saber se está a rever um rascunho gerado por IA (precisa de
 * mais atenção) ou uma frase puramente determinística.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedacaoNotaDTO {

    private String texto;

    private String fonte;
}
