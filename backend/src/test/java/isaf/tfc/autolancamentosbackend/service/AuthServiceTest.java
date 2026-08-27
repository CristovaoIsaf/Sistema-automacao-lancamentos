package isaf.tfc.autolancamentosbackend.service;

import isaf.tfc.autolancamentosbackend.model.Role;
import isaf.tfc.autolancamentosbackend.model.User;
import isaf.tfc.autolancamentosbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Peça "AuthService" do desenvolvimento incremental — testa isoladamente
 * (UserRepository mockado) o único método desta classe: a ponte entre o
 * email usado no login e o UserDetails que o Spring Security espera
 * (JwtAuthFilter/AuthenticationManager consomem isto indiretamente).
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        authService = new AuthService(userRepository);
    }

    @Test
    void loadUserByUsername_utilizadorExistente_devolveOMesmoUser() {
        User user = new User();
        user.setEmail("ana@empresa.ao");
        user.setSenha("hash-bcrypt");
        user.setStatus("ATIVO");
        user.setPapel(Role.CONTABILISTA);
        when(userRepository.findByEmail("ana@empresa.ao")).thenReturn(Optional.of(user));

        UserDetails resultado = authService.loadUserByUsername("ana@empresa.ao");

        assertThat(resultado).isSameAs(user);
        assertThat(resultado.getUsername()).isEqualTo("ana@empresa.ao");
    }

    @Test
    void loadUserByUsername_utilizadorInexistente_lancaUsernameNotFoundException() {
        when(userRepository.findByEmail("fantasma@empresa.ao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("fantasma@empresa.ao"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma@empresa.ao");
    }
}
