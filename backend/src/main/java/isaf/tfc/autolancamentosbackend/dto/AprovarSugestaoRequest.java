package isaf.tfc.autolancamentosbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Corpo opcional de POST /analises/{id}/aprovar — permite ao contabilista
 * aprovar a Sugestao exatamente como ficou revista no ecrã (as mesmas linhas
 * mostradas em LancamentoDiario.tsx), em vez de reler linhasJson tal como
 * foi gravado no passo de análise.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AprovarSugestaoRequest {

    private List<LinhaLancamentoDTO> linhas;

    // true quando o contabilista alterou algo antes de aprovar — grava-se em
    // Lancamento.editadoManualmente. Boolean (não boolean primitivo) porque
    // um corpo "{}" sem este campo tem de desserializar sem rebentar.
    private Boolean editado;

    public boolean isEditado() {
        return Boolean.TRUE.equals(editado);
    }
}
