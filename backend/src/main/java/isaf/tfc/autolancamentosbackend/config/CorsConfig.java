package isaf.tfc.autolancamentosbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * allowed.origin aceita uma lista separada por vírgulas (ver
 * application.properties) — antes só um único URL cabia aqui, o que
 * partia assim que o frontend passou a ser servido a partir de mais do
 * que um sítio ao mesmo tempo (ex.: localhost:5173 em dev + frontend
 * publicado na Vercel, ambos a falar com o mesmo backend local exposto
 * por um túnel ngrok).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${allowed.origin}")
    private String allowedOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origens = allowedOrigin.split(",");
        for (int i = 0; i < origens.length; i++) {
            origens[i] = origens[i].trim();
        }

        registry.addMapping("/**")
                .allowedOrigins(origens)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}