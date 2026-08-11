package isaf.tfc.autolancamentosbackend.controller;

import isaf.tfc.autolancamentosbackend.dto.RegistoRequestDTO;
import isaf.tfc.autolancamentosbackend.model.Empresa;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.EmpresaRepository;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import isaf.tfc.autolancamentosbackend.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        empresaRepository = Mockito.mock(EmpresaRepository.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        controller = new AuthController(userRepository, empresaRepository, jwtUtil, passwordEncoder);

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
}
