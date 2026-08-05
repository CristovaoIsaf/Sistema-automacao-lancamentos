package isaf.tfc.autolancamentosbackend.model;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Peça 2 do bloco "Autenticação/utilizadores": User.getAuthorities()
 * deixou de devolver sempre uma lista vazia — agora expõe o papel real
 * como "ROLE_&lt;PAPEL&gt;", que é o que hasRole()/@PreAuthorize do Spring
 * Security precisam para funcionar (ver JwtAuthFilter, que usa
 * user.getAuthorities() ao construir o Authentication).
 */
class UserTest {

    @Test
    void getAuthorities_utilizadorAdministrador_devolveRoleAdministrador() {
        User user = new User();
        user.setPapel(Role.ADMINISTRADOR);

        Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMINISTRADOR");
    }

    @Test
    void getAuthorities_utilizadorAuditor_devolveRoleAuditor() {
        User user = new User();
        user.setPapel(Role.AUDITOR);

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AUDITOR");
    }

    @Test
    void getAuthorities_semPapelDefinido_devolveListaVaziaSemLancarExcecao() {
        User user = new User();
        user.setPapel(null);

        assertThat(user.getAuthorities()).isEmpty();
    }
}
