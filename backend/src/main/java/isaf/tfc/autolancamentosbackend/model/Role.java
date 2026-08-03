package isaf.tfc.autolancamentosbackend.model;

/**
 * Exactamente os 3 perfis definidos pelo TFC (ver Perfil em
 * frontend/src/app/types/contabilidade.ts). Os nomes têm de bater certo
 * com os do frontend — o login real (AuthContext.login) faz
 * `perfil = resp.papel` sem qualquer tradução.
 */
public enum Role {
    ADMINISTRADOR,
    CONTABILISTA,
    AUDITOR
}
