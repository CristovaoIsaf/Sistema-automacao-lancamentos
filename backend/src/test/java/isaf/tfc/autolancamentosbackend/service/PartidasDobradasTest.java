package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.LinhaLancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Peça 1 do bloco "Partidas dobradas / IVA" — testa PartidasDobradas
 * isoladamente (classe estática pura, sem Spring, sem BD). Ver
 * DocumentoRepositoryTest/DocumentoControllerTest para o mesmo padrão
 * aplicado à peça "Documento".
 */
class PartidasDobradasTest {

    // --- parseValor -----------------------------------------------------

    @Test
    void parseValor_formatoNormalizadoComPonto_convertaCorretamente() {
        assertThat(PartidasDobradas.parseValor("150000.00")).isEqualByComparingTo("150000.00");
    }

    @Test
    void parseValor_formatoPtAoComVirgulaEMilhares_convertaCorretamente() {
        assertThat(PartidasDobradas.parseValor("50.000,00")).isEqualByComparingTo("50000.00");
    }

    @Test
    void parseValor_nuloOuEmBranco_devolveZero() {
        assertThat(PartidasDobradas.parseValor(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(PartidasDobradas.parseValor("")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(PartidasDobradas.parseValor("   ")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void parseValor_textoNaoNumerico_devolveZeroEmVezDeFalhar() {
        assertThat(PartidasDobradas.parseValor("indisponível")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- parseValorNullable ----------------------------------------------

    @Test
    void parseValorNullable_nuloOuEmBranco_devolveNull() {
        assertThat(PartidasDobradas.parseValorNullable(null)).isNull();
        assertThat(PartidasDobradas.parseValorNullable("")).isNull();
    }

    @Test
    void parseValorNullable_valorValido_convertaComoParseValor() {
        assertThat(PartidasDobradas.parseValorNullable("6140.35")).isEqualByComparingTo("6140.35");
    }

    // --- validarEquilibrio -------------------------------------------------

    @Test
    void validarEquilibrio_linhasEquilibradas_naoLancaExcecao() {
        List<LinhaLancamento> linhas = List.of(
                linha("31", new BigDecimal("50000.00"), null),
                linha("61", null, new BigDecimal("43859.65")),
                linha("34.5.2", null, new BigDecimal("6140.35"))
        );

        assertThatCode(linhas);
    }

    @Test
    void validarEquilibrio_linhasDesequilibradas_lancaExcecao() {
        List<LinhaLancamento> linhas = List.of(
                linha("31", new BigDecimal("50000.00"), null),
                linha("61", null, new BigDecimal("40000.00"))
        );

        assertThatThrownBy(() -> PartidasDobradas.validarEquilibrio(linhas))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não está equilibrado");
    }

    @Test
    void validarEquilibrio_listaVazia_naoLancaExcecao() {
        assertThatCode(List.of());
    }

    @Test
    void validarEquilibrio_umaSoLinhaComDebitoSemContrapartida_lancaExcecao() {
        List<LinhaLancamento> linhas = List.of(linha("31", new BigDecimal("50000.00"), null));

        assertThatThrownBy(() -> PartidasDobradas.validarEquilibrio(linhas))
                .isInstanceOf(RuntimeException.class);
    }

    private void assertThatCode(List<LinhaLancamento> linhas) {
        org.assertj.core.api.Assertions.assertThatCode(() -> PartidasDobradas.validarEquilibrio(linhas))
                .doesNotThrowAnyException();
    }

    private LinhaLancamento linha(String conta, BigDecimal debito, BigDecimal credito) {
        LinhaLancamento linha = new LinhaLancamento();
        linha.setConta(conta);
        linha.setDebito(debito);
        linha.setCredito(credito);
        return linha;
    }
}
