package isaf.tfc.autolancamentosbackend.config;

import isaf.tfc.autolancamentosbackend.AutoLancamentosBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Auditoria C11: jwt.secret deixou de ter um valor por omissão fora do
 * perfil "dev" (ver application.properties / application-dev.properties) —
 * um JWT_SECRET em falta em produção tem de impedir o arranque, nunca
 * assinar tokens com o valor commitado no repositório. Sobe a aplicação
 * real (SpringApplicationBuilder, mesma classe de arranque de produção),
 * fora do perfil "dev" e sem JWT_SECRET, e confirma que falha por causa do
 * placeholder em falta especificamente — não por qualquer outro motivo.
 *
 * Web server desligado (WebApplicationType.NONE) só para isolar este teste
 * de portas ocupadas e de autoconfiguração MVC — o caminho "perfil dev
 * arranca sem configurar nada" já tem cobertura real, com servidor web
 * completo, em AutoLancamentosBackendApplicationTests/DocumentoRbacTest/
 * AnaliseRbacTest (todos @SpringBootTest, perfil "dev" por omissão).
 */
class JwtSecretRequiredEmProducaoTest {

    @Test
    void semPerfilDevESemJwtSecret_falhaAoArrancarPorCausaDoPlaceholder() {
        // SPRING_PROFILES_ACTIVE como system property (não .profiles() do
        // builder) para reproduzir fielmente o mecanismo real de produção
        // (ver render.yaml — a mesma variável de ambiente): é lida por
        // spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev} em
        // application.properties antes de qualquer perfil ser resolvido.
        System.setProperty("SPRING_PROFILES_ACTIVE", "prod");
        try {
            SpringApplicationBuilder app = new SpringApplicationBuilder(AutoLancamentosBackendApplication.class)
                    .web(WebApplicationType.NONE);

            assertThatThrownBy(app::run)
                    .rootCause()
                    .hasMessageContaining("JWT_SECRET");
        } finally {
            System.clearProperty("SPRING_PROFILES_ACTIVE");
        }
    }
}
