package isaf.tfc.autolancamentosbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false, length = 14)
    private String nif;

    @Column(nullable = false)
    private String status;



    @Column(nullable = false)
    private String senha;   // será armazenada com hash (bcrypt)

    @Enumerated(EnumType.STRING)
    private Role papel;

    // Aditivo (Fase 1 — modelo de empresa/utilizadores): o utilizador
    // pertence ao contexto da empresa da instalação. Sem relação JPA, para
    // manter o padrão já usado nas outras entidades (ver DocumentoContabilistico).
    // Preenchido automaticamente com a empresa única em UserService.criar —
    // este projecto é single-tenant, não multi-empresa.
    private Long empresaId;


    // Antes devolvia sempre List.of() — nenhum papel era exposto ao Spring
    // Security, por isso hasRole()/@PreAuthorize nunca tinham nada para
    // verificar (RBAC ficava só do lado da interface, ver Layout.tsx).
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (papel == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
