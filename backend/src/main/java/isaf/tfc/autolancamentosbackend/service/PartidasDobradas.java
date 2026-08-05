package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parsing de valores monetários e validação de equilíbrio débito/crédito —
 * extraído de AnaliseContabilService para ser testável isoladamente (peça
 * central do desenvolvimento incremental: sem isto, um lançamento
 * desequilibrado nunca deveria chegar a ser gravado).
 */
public final class PartidasDobradas {

    private PartidasDobradas() {
    }

    /**
     * Converte o valor em texto devolvido pela API de análise para BigDecimal.
     *
     * A API de análise já devolve os valores normalizados com ponto decimal
     * (ex: "150000.00" — ver pgc.py::_dec). Só trata "." como separador de
     * milhares quando também há vírgula no texto (formato PT-AO "50.000,00"),
     * para não estragar valores que já vêm corretos.
     */
    public static BigDecimal parseValor(String valorTexto) {
        if (valorTexto == null || valorTexto.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpo = valorTexto.replaceAll("[^0-9,.-]", "");
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Como parseValor, mas devolve null (em vez de ZERO) quando o texto está
     * vazio — necessário para preservar null no lado da linha que não é usado
     * (débito OU crédito), tal como o pgc_ao já faz.
     */
    public static BigDecimal parseValorNullable(String valorTexto) {
        if (valorTexto == null || valorTexto.isBlank()) {
            return null;
        }
        return parseValor(valorTexto);
    }

    /**
     * Verificação defensiva: as linhas devolvidas pela API de análise já
     * deviam estar equilibradas (pgc_ao valida isso do lado do FastAPI), mas
     * confirma-se aqui também antes de gravar o Lancamento oficial.
     */
    public static void validarEquilibrio(List<LinhaLancamento> linhas) {
        BigDecimal totalDebito = linhas.stream()
                .map(l -> l.getDebito() != null ? l.getDebito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredito = linhas.stream()
                .map(l -> l.getCredito() != null ? l.getCredito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebito.compareTo(totalCredito) != 0) {
            throw new RuntimeException("O lançamento automático não está equilibrado.");
        }
    }
}
