package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.dto.UserRequestDTO;
import isaf.tfc.autolancamentosbackend.dto.UserResponseDTO;
import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Auditoria C08: UserService.apagar isolado (repositório/PasswordEncoder/
 * EmpresaService mockados) — confirma que "apagar" um utilizador passou a
 * ser um soft-delete (status=INATIVO), nunca um DELETE físico, para não
 * destruir a identidade de quem fez o quê no histórico derivado (ver
 * AuditoriaService).
 */
class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        EmpresaService empresaService = Mockito.mock(EmpresaService.class);
        service = new UserService(userRepository, passwordEncoder, empresaService);

        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private User utilizador(Long id, String status) {
        User user = new User();
        user.setId(id);
        user.setNome("Ana Contabilista");
        user.setEmail("ana@empresa.ao");
        user.setNif("5000000002LA");
        user.setStatus(status);
        user.setPapel(Role.CONTABILISTA);
        return user;
    }

    @Test
    void apagar_nuncaChamaDeleteFisico() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilizador(1L, "ATIVO")));

        service.apagar(1L);

        verify(userRepository, never()).delete(any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void apagar_marcaComoInativoEPreservaOsRestantesCampos() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilizador(1L, "ATIVO")));

        service.apagar(1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("INATIVO");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@empresa.ao");
        assertThat(captor.getValue().getNome()).isEqualTo("Ana Contabilista");
    }

    @Test
    void atualizar_semStatusNoPedido_mantemOStatusAtual() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilizador(1L, "INATIVO")));

        UserRequestDTO dados = new UserRequestDTO();
        dados.setNome("Ana Contabilista");
        dados.setEmail("ana@empresa.ao");
        dados.setNif("5000000002LA");
        dados.setPapel(Role.CONTABILISTA);

        UserResponseDTO resposta = service.atualizar(1L, dados);

        assertThat(resposta.getStatus()).isEqualTo("INATIVO");
    }

    @Test
    void atualizar_comStatusAtivoNoPedido_reativaOUtilizador() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(utilizador(1L, "INATIVO")));

        UserRequestDTO dados = new UserRequestDTO();
        dados.setNome("Ana Contabilista");
        dados.setEmail("ana@empresa.ao");
        dados.setNif("5000000002LA");
        dados.setPapel(Role.CONTABILISTA);
        dados.setStatus("ATIVO");

        UserResponseDTO resposta = service.atualizar(1L, dados);

        assertThat(resposta.getStatus()).isEqualTo("ATIVO");
    }
}
