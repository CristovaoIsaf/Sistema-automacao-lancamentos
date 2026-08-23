package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.LoginRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.LoginResponseDTO;
import isaf.tfc.autolancamentosbackend.dto.RegistoRequestDTO;
import isaf.tfc.autolancamentosbackend.model.Empresa;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.EmpresaRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import isaf.tfc.autolancamentosbackend.security.JwtUtil;
import isaf.tfc.autolancamentosbackend.security.LoginRateLimiter;
import isaf.tfc.autolancamentosbackend.service.AuditLogService;
import isaf.tfc.autolancamentosbackend.service.TwoFactorAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fase 20 do plano de 20 fases — "registo público". AuthController
 * isolado (repositórios/JwtUtil/PasswordEncoder mockados), mesmo padrão
 * dos testes de serviço já existentes — só que aqui é o próprio
 * controller a conter a lógica (AuthController nunca delegou a um
 * AuthService).
 */
class AuthControllerTest {

    private UserRepository userRepository;
    private EmpresaRepository empresaRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;
    private AuditLogService auditLogService;
    private TwoFactorAuthService twoFactorAuthService;
    private LoginRateLimiter loginRateLimiter;
    private HttpServletRequest httpRequest;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        twoFactorAuthService = Mockito.mock(TwoFactorAuthService.class);
        // Real, não mockado — testa a integração de facto (ver
        // LoginRateLimiterTest.java para o comportamento isolado do
        // limitador), e cada teste começa com um limitador novo/limpo.
        loginRateLimiter = new LoginRateLimiter();
        httpRequest = Mockito.mock(HttpServletRequest.class);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        controller = new AuthController(userRepository, empresaRepository, jwtUtil, passwordEncoder, auditLogService, twoFactorAuthService, loginRateLimiter);

        when(passwordEncoder.encode(any())).thenReturn("hash-fictício");
        when(jwtUtil.generateToken(any(), any())).thenReturn("token-fictício");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private RegistoRequestDTO dados() {
        RegistoRequestDTO dto = new RegistoRequestDTO();
        dto.setNome("Cristóvão Cuzonda");
        dto.setNif("5000000001LA");
        dto.setEmail("admin@empresa.ao");
        dto.setSenha("senhaSegura123");
        dto.setNomeEmpresa("Empresa Teste Lda");
        dto.setNifEmpresa("5000123456LA");
        return dto;
    }

    @Test
    void registar_semUtilizadoresENenhumaEmpresa_criaEmpresaEAdministrador() {
        when(userRepository.count()).thenReturn(0L);
        when(empresaRepository.findAll()).thenReturn(List.of());
        when(empresaRepository.save(any())).thenAnswer(inv -> {
            Empresa e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        ResponseEntity<?> resposta = controller.registar(dados());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        assertThat(empresaCaptor.getValue().getNome()).isEqualTo("Empresa Teste Lda");
        assertThat(empresaCaptor.getValue().getNif()).isEqualTo("5000123456LA");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPapel()).isEqualTo(Role.ADMINISTRADOR);
        assertThat(userCaptor.getValue().getEmpresaId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@empresa.ao");
    }

    @Test
    void registar_comEmpresaJaSemeada_naoCriaSegundaEmpresaSoAnexaOAdministrador() {
        when(userRepository.count()).thenReturn(0L);
        Empresa existente = new Empresa();
        existente.setId(7L);
        existente.setNome("Empresa Já Semeada");
        when(empresaRepository.findAll()).thenReturn(List.of(existente));

        ResponseEntity<?> resposta = controller.registar(dados());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(empresaRepository, never()).save(any());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmpresaId()).isEqualTo(7L);
    }

    @Test
    void registar_comUtilizadoresJaExistentes_devolve409ENaoCriaNada() {
        when(userRepository.count()).thenReturn(1L);

        ResponseEntity<?> resposta = controller.registar(dados());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
        verify(empresaRepository, never()).save(any());
    }

    @Test
    void registar_encriptaASenhaAntesDeGravar() {
        when(userRepository.count()).thenReturn(0L);
        when(empresaRepository.findAll()).thenReturn(List.of(new Empresa()));

        controller.registar(dados());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getSenha()).isEqualTo("hash-fictício");
        verify(passwordEncoder).encode("senhaSegura123");
    }

    private LoginRequestDTO loginDados() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("ana@empresa.ao");
        dto.setPassword("senhaCorreta123");
        return dto;
    }

    private User utilizadorComStatus(String status) {
        User user = new User();
        user.setNome("Ana Contabilista");
        user.setEmail("ana@empresa.ao");
        user.setNif("5000000002LA");
        user.setStatus(status);
        user.setSenha("hash-armazenado");
        user.setPapel(Role.CONTABILISTA);
        return user;
    }

    // Auditoria C09: um utilizador suspenso (status != "ATIVO") não pode
    // iniciar sessão, mesmo com a password correta — antes desta correção,
    // login() nunca olhava para "status" (só existia no modelo, nunca era
    // lido em lado nenhum), por isso suspender um utilizador não tinha
    // nenhum efeito prático.
    @Test
    void login_utilizadorInativo_rejeitaMesmoComPasswordCorreta() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("INATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);

        assertThatThrownBy(() -> controller.login(loginDados(), httpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inativo");

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_utilizadorAtivoComPasswordCorreta_devolveToken() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(jwtUtil.generateToken("ana@empresa.ao", Role.CONTABILISTA)).thenReturn("token-valido");

        ResponseEntity<LoginResponseDTO> resposta = controller.login(loginDados(), httpRequest);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().getToken()).isEqualTo("token-valido");
    }

    // --- Auditoria C06/C07 — login passa a ficar sempre registado --------

    @Test
    void login_comSucesso_registaAuditLogDeSucesso() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);

        controller.login(loginDados(), httpRequest);

        verify(auditLogService).registar(any(), org.mockito.ArgumentMatchers.eq("LOGIN"), any(), any(),
                org.mockito.ArgumentMatchers.eq(AuditLogService.SUCESSO), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("127.0.0.1"));
    }

    @Test
    void login_comPasswordErrada_registaAuditLogDeFalha() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(loginDados(), httpRequest)).isInstanceOf(RuntimeException.class);

        verify(auditLogService).registar(any(), org.mockito.ArgumentMatchers.eq("LOGIN"), any(), any(),
                org.mockito.ArgumentMatchers.eq(AuditLogService.FALHA), any(), org.mockito.ArgumentMatchers.eq("127.0.0.1"));
    }

    @Test
    void login_utilizadorInexistente_registaAuditLogComOEmailTentado() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(loginDados(), httpRequest)).isInstanceOf(RuntimeException.class);

        verify(auditLogService).registarComEmail(org.mockito.ArgumentMatchers.eq("ana@empresa.ao"),
                org.mockito.ArgumentMatchers.eq("LOGIN"), org.mockito.ArgumentMatchers.eq(AuditLogService.FALHA),
                any(), org.mockito.ArgumentMatchers.eq("127.0.0.1"));
    }

    // --- 2FA -------------------------------------------------------------

    private User utilizadorCom2FA() {
        User user = utilizadorComStatus("ATIVO");
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("segredo-fictício");
        return user;
    }

    @Test
    void login_comDoisFactoresAtivosESemCodigo_devolveRequiresTwoFactorSemToken() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorCom2FA()));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);

        ResponseEntity<LoginResponseDTO> resposta = controller.login(loginDados(), httpRequest);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody().isRequiresTwoFactor()).isTrue();
        assertThat(resposta.getBody().getToken()).isNull();
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_comDoisFactoresAtivosECodigoErrado_lancaExcecaoENaoEmiteToken() {
        LoginRequestDTO dados = loginDados();
        dados.setCodigo2FA("000000");
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorCom2FA()));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(twoFactorAuthService.verificarLoginComDoisFactores(any(), org.mockito.ArgumentMatchers.eq("000000")))
                .thenReturn(false);

        assertThatThrownBy(() -> controller.login(dados, httpRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Código de autenticação inválido");

        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_comDoisFactoresAtivosECodigoCorreto_emiteToken() {
        LoginRequestDTO dados = loginDados();
        dados.setCodigo2FA("123456");
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorCom2FA()));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(twoFactorAuthService.verificarLoginComDoisFactores(any(), org.mockito.ArgumentMatchers.eq("123456")))
                .thenReturn(true);
        when(jwtUtil.generateToken("ana@empresa.ao", Role.CONTABILISTA)).thenReturn("token-valido");

        ResponseEntity<LoginResponseDTO> resposta = controller.login(dados, httpRequest);

        assertThat(resposta.getBody().isRequiresTwoFactor()).isFalse();
        assertThat(resposta.getBody().getToken()).isEqualTo("token-valido");
    }

    @Test
    void login_semDoisFactoresAtivos_naoChamaTwoFactorAuthService() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);

        controller.login(loginDados(), httpRequest);

        verify(twoFactorAuthService, never()).verificarLoginComDoisFactores(any(), any());
    }

    // --- Rate limiting -----------------------------------------------------
    // Auditoria de segurança: confirmado ao vivo que /auth/login aceitava
    // tentativas ilimitadas de password e de código 2FA sem qualquer
    // atraso — ver LoginRateLimiter.java.

    @Test
    void login_apos5PasswordsErradas_bloqueiaMesmoComPasswordCorretaDepois() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(passwordEncoder.matches("errada", "hash-armazenado")).thenReturn(false);

        LoginRequestDTO tentativaErrada = loginDados();
        tentativaErrada.setPassword("errada");
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> controller.login(tentativaErrada, httpRequest)).isInstanceOf(RuntimeException.class);
        }

        assertThatThrownBy(() -> controller.login(loginDados(), httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_apos5CodigosDoisFatoresErrados_bloqueiaMesmoComCodigoCorretoDepois() {
        LoginRequestDTO dados = loginDados();
        dados.setCodigo2FA("000000");
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorCom2FA()));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(twoFactorAuthService.verificarLoginComDoisFactores(any(), org.mockito.ArgumentMatchers.eq("000000")))
                .thenReturn(false);
        when(twoFactorAuthService.verificarLoginComDoisFactores(any(), org.mockito.ArgumentMatchers.eq("123456")))
                .thenReturn(true);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> controller.login(dados, httpRequest)).isInstanceOf(RuntimeException.class);
        }

        LoginRequestDTO comCodigoCorreto = loginDados();
        comCodigoCorreto.setCodigo2FA("123456");
        assertThatThrownBy(() -> controller.login(comCodigoCorreto, httpRequest))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    void login_comSucesso_limpaContadorDeTentativasFalhadas() {
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(utilizadorComStatus("ATIVO")));
        when(passwordEncoder.matches("senhaCorreta123", "hash-armazenado")).thenReturn(true);
        when(passwordEncoder.matches("errada", "hash-armazenado")).thenReturn(false);

        LoginRequestDTO tentativaErrada = loginDados();
        tentativaErrada.setPassword("errada");
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> controller.login(tentativaErrada, httpRequest)).isInstanceOf(RuntimeException.class);
        }

        // login com sucesso no meio — limpa o contador
        controller.login(loginDados(), httpRequest);

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> controller.login(tentativaErrada, httpRequest)).isInstanceOf(RuntimeException.class);
        }

        // só 3 falhas desde o último sucesso — ainda não deve bloquear
        ResponseEntity<LoginResponseDTO> resposta = controller.login(loginDados(), httpRequest);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
