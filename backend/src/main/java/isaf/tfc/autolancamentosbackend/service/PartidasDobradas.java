package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parsing de valores monetários, construção de linhas e validação de
 * equilíbrio débito/crédito — extraído de AnaliseContabilService para ser
 * testável isoladamente (peça central do desenvolvimento incremental: sem
 * isto, um lançamento desequilibrado nunca deveria chegar a ser gravado).
 *
 * Fase 7 do plano de 20 fases ("única fonte de verdade para lançamentos,
 * mas todos devem passar pelo mesmo domínio"): construirLinhas/
 * validarEquilibrio são agora o único ponto onde uma LinhaLancamentoDTO
 * (origem MANUAL — ver LancamentoServiceImpl — ou revisão do contabilista
 * antes de aprovar uma Sugestao — ver AnaliseContabilService) vira uma
 * LinhaLancamento persistível. Antes desta fase, LancamentoServiceImpl
 * mantinha a sua própria validarEquilibrio (reimplementação da mesma soma
 * débito/crédito) e nunca copiava linhaDTO.descricao para a entidade — os
 * lançamentos manuais ficavam sempre com LinhaLancamento.descricao null.
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
     * Verificação defensiva antes de gravar QUALQUER Lancamento, seja de
     * origem AUTOMATICO (a API de análise já devia ter devolvido linhas
     * equilibradas — pgc_ao valida isso do lado do FastAPI) ou MANUAL (o
     * contabilista pode ter-se enganado a preencher débito/crédito).
     */
    public static void validarEquilibrio(List<LinhaLancamento> linhas) {
        BigDecimal totalDebito = linhas.stream()
                .map(l -> l.getDebito() != null ? l.getDebito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredito = linhas.stream()
                .map(l -> l.getCredito() != null ? l.getCredito() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebito.compareTo(totalCredito) != 0) {
            throw new RuntimeException("O lançamento não está equilibrado.");
        }
    }

    /**
     * Converte uma LinhaLancamentoDTO (origem MANUAL ou revisão do
     * contabilista antes de aprovar uma Sugestao) numa LinhaLancamento
     * persistível. `descricaoDefeito` é usada quando a própria linha não
     * traz descrição (ex: o contabilista só preencheu conta/valor) — o
     * mesmo padrão já usado em AnaliseContabilService antes desta fase.
     */
    public static List<LinhaLancamento> construirLinhas(List<LinhaLancamentoDTO> dtos, String descricaoDefeito) {
        return dtos.stream().map(dto -> {
            LinhaLancamento linha = new LinhaLancamento();
            linha.setConta(dto.getConta());
            linha.setDebito(dto.getDebito());
            linha.setCredito(dto.getCredito());
            linha.setDescricao(dto.getDescricao() != null ? dto.getDescricao() : descricaoDefeito);
            return linha;
        }).toList();
    }
}
