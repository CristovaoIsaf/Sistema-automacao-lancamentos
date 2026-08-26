package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.ContaDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaLancamentoDTO;
import isaf.tfc.autolancamentosbackend.dto.LinhaSugeridaDTO;
import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Extensão do projeto às contas do PGC-AO (34.5.1 = IVA dedutível,
    // 34.5.2 = IVA liquidado) — ver fastapi/app/services/pgc.py. O FastAPI
    // já devolve linhas nestas contas nas partidas dobradas sugeridas;
    // antes desta modelação o Java nunca reconhecia essas linhas como IVA
    // especificamente, o valor ficava só "enterrado" numa conta qualquer
    // dentro de linhasJson/LinhaLancamento.
    public static final String PREFIXO_CONTA_IVA = "34.5";

    /**
     * Soma o valor das linhas de IVA (conta a começar por PREFIXO_CONTA_IVA)
     * de um lançamento já construído — usa o lado preenchido (débito OU
     * crédito) de cada linha, nunca os dois. Devolve ZERO quando não há
     * nenhuma linha de IVA, nunca null, para o campo persistido nunca
     * precisar de tratar "sem IVA" como um caso especial separado de "0".
     */
    public static BigDecimal calcularValorIva(List<LinhaLancamento> linhas) {
        return linhas.stream()
                .filter(PartidasDobradas::ehLinhaIva)
                .map(linha -> valorPreenchido(linha.getDebito(), linha.getCredito()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean ehLinhaIva(LinhaLancamento linha) {
        return linha.getConta() != null && linha.getConta().startsWith(PREFIXO_CONTA_IVA);
    }

    /**
     * Mesmo cálculo que calcularValorIva(List&lt;LinhaLancamento&gt;), mas
     * sobre as linhas ainda em formato texto tal como a API de análise as
     * devolve (LinhaSugeridaDTO) — usado ao criar a Sugestao, antes de
     * qualquer linha virar LinhaLancamento persistível.
     */
    public static BigDecimal calcularValorIvaSugerido(List<LinhaSugeridaDTO> linhas) {
        if (linhas == null) {
            return BigDecimal.ZERO;
        }
        return linhas.stream()
                .filter(linha -> linha.getConta() != null && linha.getConta().startsWith(PREFIXO_CONTA_IVA))
                .map(linha -> valorPreenchido(parseValorNullable(linha.getDebito()), parseValorNullable(linha.getCredito())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal valorPreenchido(BigDecimal debito, BigDecimal credito) {
        if (debito != null) {
            return debito;
        }
        return credito != null ? credito : BigDecimal.ZERO;
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
     * Auditoria C14: até esta correção, nenhum ponto de escrita validava
     * que a conta de uma linha existe no plano de contas (PGC-AO) — um
     * lançamento podia ser gravado com um código inventado ou mal escrito.
     * `planoDeContas` fica a cargo de quem chama (ver PlanoContasClient) —
     * PartidasDobradas continua sem depender do Spring. Deliberadamente
     * "fail-open": se o plano de contas ainda não foi carregado (FastAPI
     * em baixo — ver PlanoContasClient.tentarCarregar), uma lista vazia
     * salta a validação em vez de bloquear TODOS os lançamentos por uma
     * dependência externa estar indisponível.
     */
    public static void validarContasExistem(List<LinhaLancamento> linhas, List<ContaDTO> planoDeContas) {
        if (planoDeContas == null || planoDeContas.isEmpty()) {
            return;
        }
        Set<String> codigosValidos = planoDeContas.stream()
                .map(ContaDTO::getCodigo)
                .collect(Collectors.toSet());

        List<String> invalidas = linhas.stream()
                .map(LinhaLancamento::getConta)
                .filter(conta -> conta == null || !codigosValidos.contains(conta))
                .map(conta -> conta == null ? "(vazia)" : conta)
                .distinct()
                .toList();

        if (!invalidas.isEmpty()) {
            throw new RuntimeException("Conta inexistente no plano de contas: " + String.join(", ", invalidas));
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
