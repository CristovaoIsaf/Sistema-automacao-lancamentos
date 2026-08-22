package isaf.tfc.autolancamentosbackend.service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import isaf.tfc.autolancamentosbackend.dto.TwoFactorSetupResponseDTO;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Autenticação de dois factores (TOTP, RFC 6238) — cada utilizador ativa a
 * sua própria (opt-in, ninguém é obrigado), independente do papel
 * (ADMINISTRADOR/CONTABILISTA/AUDITOR protegem a própria conta da mesma
 * forma). O QR code em si é gerado no frontend a partir do URI
 * `otpauthUrl` devolvido por iniciarSetup() — este serviço nunca gera uma
 * imagem, só o segredo e os códigos.
 */
@Service
public class TwoFactorAuthService {

    private static final int NUMERO_CODIGOS_RECUPERACAO = 8;
    private static final String EMISSOR = "AutoLancamentos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier;

    public TwoFactorAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /**
     * Passo 1: gera um novo segredo e guarda-o já (ainda sem ativar — ver
     * User.twoFactorEnabled) para o utilizador poder tentar o setup mais
     * do que uma vez sem ter de recomeçar do zero se falhar a confirmar.
     */
    public TwoFactorSetupResponseDTO iniciarSetup(User user) {
        if (user.isTwoFactorEnabled()) {
            throw new RuntimeException("A autenticação de dois factores já está ativa nesta conta.");
        }
        String secret = secretGenerator.generate();
        user.setTwoFactorSecret(secret);
        userRepository.save(user);
        return new TwoFactorSetupResponseDTO(secret, gerarUriProvisionamento(user.getEmail(), secret));
    }

    /**
     * Passo 2: confirma que o utilizador configurou a app autenticadora
     * corretamente (um código válido prova que o segredo foi lido bem) e só
     * então ativa a exigência no login. Gera os códigos de recuperação
     * nesse mesmo momento — nunca antes, para não gerar códigos "órfãos"
     * de um setup nunca confirmado.
     */
    public List<String> confirmarSetup(User user, String codigo) {
        if (user.getTwoFactorSecret() == null) {
            throw new RuntimeException("Não há nenhum setup de dois factores em curso — chama primeiro POST /api/2fa/setup.");
        }
        if (user.isTwoFactorEnabled()) {
            throw new RuntimeException("A autenticação de dois factores já está ativa nesta conta.");
        }
        if (!verificarCodigoTotp(user.getTwoFactorSecret(), codigo)) {
            throw new RuntimeException("Código de autenticação inválido.");
        }

        List<String> codigosRecuperacao = gerarCodigosRecuperacao();
        List<String> hashes = codigosRecuperacao.stream().map(passwordEncoder::encode).toList();

        user.setTwoFactorEnabled(true);
        user.setTwoFactorRecoveryCodesJson(serializar(hashes));
        userRepository.save(user);

        return codigosRecuperacao;
    }

    /**
     * Exige a password atual — um token JWT roubado sozinho não pode
     * desligar a proteção extra que os dois factores davam precisamente
     * contra esse cenário.
     */
    public void desativar(User user, String password) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Password inválida.");
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setTwoFactorRecoveryCodesJson(null);
        userRepository.save(user);
    }

    /**
     * Usado no login (ver AuthController): aceita tanto um código TOTP de
     * 6 dígitos como um código de recuperação. Um código de recuperação
     * válido é consumido (removido da lista) mesmo que o login falhe
     * depois por outro motivo qualquer — assim que é gasto, é gasto, para
     * não permitir reutilização se alguém o intercetar.
     */
    public boolean verificarLoginComDoisFactores(User user, String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        if (verificarCodigoTotp(user.getTwoFactorSecret(), codigo)) {
            return true;
        }
        return consumirCodigoRecuperacao(user, codigo);
    }

    private boolean verificarCodigoTotp(String secret, String codigo) {
        if (secret == null || codigo == null || codigo.isBlank()) {
            return false;
        }
        try {
            return codeVerifier.isValidCode(secret, codigo.trim());
        } catch (Exception e) {
            // Código em formato inválido (ex.: não numérico) — trata como
            // "não bate certo", nunca como erro de servidor.
            return false;
        }
    }

    private boolean consumirCodigoRecuperacao(User user, String codigo) {
        List<String> hashes = desserializar(user.getTwoFactorRecoveryCodesJson());
        for (String hash : hashes) {
            if (passwordEncoder.matches(codigo.trim(), hash)) {
                hashes.remove(hash);
                user.setTwoFactorRecoveryCodesJson(serializar(hashes));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private String gerarUriProvisionamento(String emailUtilizador, String secret) {
        String issuer = URLEncoder.encode(EMISSOR, StandardCharsets.UTF_8);
        String label = URLEncoder.encode(EMISSOR + ":" + emailUtilizador, StandardCharsets.UTF_8);
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    private List<String> gerarCodigosRecuperacao() {
        SecureRandom random = new SecureRandom();
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < NUMERO_CODIGOS_RECUPERACAO; i++) {
            codigos.add(gerarCodigoRecuperacao(random));
        }
        return codigos;
    }

    // Formato XXXXX-XXXXX (10 dígitos) — fácil de escrever à mão e de
    // distinguir visualmente de um código TOTP de 6 dígitos.
    private String gerarCodigoRecuperacao(SecureRandom random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == 5) {
                sb.append('-');
            }
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private String serializar(List<String> hashes) {
        try {
            return objectMapper.writeValueAsString(hashes);
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível guardar os códigos de recuperação: " + e.getMessage(), e);
        }
    }

    private List<String> desserializar(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> lista = objectMapper.readValue(
                    json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return new ArrayList<>(lista);
        } catch (JacksonException e) {
            throw new RuntimeException("Não foi possível ler os códigos de recuperação: " + e.getMessage(), e);
        }
    }
}
