package isaf.tfc.autolancamentosbackend.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorSetupResponseDTO;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TwoFactorAuthService isolado (UserRepository mockado, PasswordEncoder
 * real — BCrypt é barato o suficiente e evita mockar comportamento de
 * hashing). Os códigos TOTP "válidos" usados nestes testes são gerados com
 * a mesma biblioteca (DefaultCodeGenerator) sobre o segredo devolvido por
 * iniciarSetup — nunca hardcoded, porque um código TOTP só é válido numa
 * janela de tempo específica.
 */
class TwoFactorAuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TwoFactorAuthService service;
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new TwoFactorAuthService(userRepository, passwordEncoder, new ObjectMapper());
    }

    private User utilizador() {
        User user = new User();
        user.setId(1L);
        user.setNome("Ana Contabilista");
        user.setEmail("ana@empresa.ao");
        user.setSenha(passwordEncoder.encode("senhaCorreta123"));
        return user;
    }

    private String codigoValidoPara(String secret) throws CodeGenerationException {
        long counter = timeProvider.getTime() / 30;
        return codeGenerator.generate(secret, counter);
    }

    @Test
    void iniciarSetup_geraSegredoEUriProvisionamento() {
        User user = utilizador();

        TwoFactorSetupResponseDTO resposta = service.iniciarSetup(user);

        assertThat(resposta.getSecret()).isNotBlank();
        assertThat(resposta.getOtpauthUrl())
                .startsWith("otpauth://totp/")
                .contains("secret=" + resposta.getSecret())
                .contains("ana%40empresa.ao");
        assertThat(user.getTwoFactorSecret()).isEqualTo(resposta.getSecret());
        // Setup por si só nunca activa a exigência no login — só confirmarSetup().
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }

    @Test
    void iniciarSetup_jaAtivo_lancaExcecao() {
        User user = utilizador();
        user.setTwoFactorEnabled(true);

        assertThatThrownBy(() -> service.iniciarSetup(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("já está ativa");
    }

    @Test
    void confirmarSetup_semSetupEmCurso_lancaExcecao() {
        User user = utilizador();

        assertThatThrownBy(() -> service.confirmarSetup(user, "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Não há nenhum setup");
    }

    @Test
    void confirmarSetup_codigoInvalido_lancaExcecaoENaoAtiva() {
        User user = utilizador();
        service.iniciarSetup(user);

        assertThatThrownBy(() -> service.confirmarSetup(user, "000000"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inválido");
        assertThat(user.isTwoFactorEnabled()).isFalse();
    }

    @Test
    void confirmarSetup_codigoValido_ativaEDevolveOitoCodigosDeRecuperacao() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);

        List<String> recuperacao = service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));

        assertThat(user.isTwoFactorEnabled()).isTrue();
        assertThat(recuperacao).hasSize(8);
        assertThat(recuperacao).allMatch(c -> c.matches("\\d{5}-\\d{5}"));
        // Guardados só como hash — nunca em texto simples na BD.
        assertThat(user.getTwoFactorRecoveryCodesJson()).doesNotContain(recuperacao.get(0));
    }

    @Test
    void verificarLoginComDoisFactores_codigoTotpValido_aceita() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);
        service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));

        boolean valido = service.verificarLoginComDoisFactores(user, codigoValidoPara(user.getTwoFactorSecret()));

        assertThat(valido).isTrue();
    }

    @Test
    void verificarLoginComDoisFactores_codigoErrado_rejeita() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);
        service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));

        assertThat(service.verificarLoginComDoisFactores(user, "000000")).isFalse();
    }

    @Test
    void verificarLoginComDoisFactores_semCodigo_rejeitaSemLancarExcecao() {
        User user = utilizador();
        service.iniciarSetup(user);

        assertThat(service.verificarLoginComDoisFactores(user, null)).isFalse();
        assertThat(service.verificarLoginComDoisFactores(user, "  ")).isFalse();
    }

    @Test
    void verificarLoginComDoisFactores_codigoRecuperacaoValido_aceitaEConsomeAoUsar() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);
        List<String> recuperacao = service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));
        String codigoRecuperacao = recuperacao.get(0);

        assertThat(service.verificarLoginComDoisFactores(user, codigoRecuperacao)).isTrue();
        // Uso único — o mesmo código não pode voltar a autenticar.
        assertThat(service.verificarLoginComDoisFactores(user, codigoRecuperacao)).isFalse();
    }

    @Test
    void desativar_passwordErrada_lancaExcecaoENaoDesativa() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);
        service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));

        assertThatThrownBy(() -> service.desativar(user, "senhaErrada"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password inválida");
        assertThat(user.isTwoFactorEnabled()).isTrue();
    }

    @Test
    void desativar_passwordCorreta_desativaELimpaSegredoECodigos() throws Exception {
        User user = utilizador();
        TwoFactorSetupResponseDTO setup = service.iniciarSetup(user);
        service.confirmarSetup(user, codigoValidoPara(setup.getSecret()));

        service.desativar(user, "senhaCorreta123");

        assertThat(user.isTwoFactorEnabled()).isFalse();
        assertThat(user.getTwoFactorSecret()).isNull();
        assertThat(user.getTwoFactorRecoveryCodesJson()).isNull();
    }
}
