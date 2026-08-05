package isaf.tfc.autolancamentosbackend.security;

import isaf.tfc.autolancamentosbackend.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Peça 1 do bloco "Autenticação/utilizadores": JwtUtil isolado (sem
 * contexto Spring — os campos @Value são injetados via
 * ReflectionTestUtils, já que não há um application.properties de teste
 * carregado aqui).
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "umaChaveSecretaBastanteLongaComPeloMenos32Bytes1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600L);
    }

    @Test
    void generateTokenEExtractEmail_devolveOMesmoEmailUsadoNaGeracao() {
        String token = jwtUtil.generateToken("teste@exemplo.com", Role.ADMINISTRADOR);

        assertThat(jwtUtil.extractEmail(token)).isEqualTo("teste@exemplo.com");
    }

    @Test
    void validateToken_tokenGeradoAgoraMesmo_ehValido() {
        String token = jwtUtil.generateToken("teste@exemplo.com", Role.CONTABILISTA);

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_tokenAdulterado_ehInvalido() {
        String token = jwtUtil.generateToken("teste@exemplo.com", Role.AUDITOR);
        String adulterado = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.validateToken(adulterado)).isFalse();
    }

    @Test
    void validateToken_textoQualquer_ehInvalido() {
        assertThat(jwtUtil.validateToken("isto-nao-e-um-jwt")).isFalse();
    }

    @Test
    void validateToken_tokenExpirado_ehInvalido() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10L);
        String tokenJaExpirado = jwtUtil.generateToken("teste@exemplo.com", Role.ADMINISTRADOR);

        assertThat(jwtUtil.validateToken(tokenJaExpirado)).isFalse();
    }
}
