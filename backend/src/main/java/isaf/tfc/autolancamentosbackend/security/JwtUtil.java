package isaf.tfc.autolancamentosbackend.security;

import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import isaf.tfc.autolancamentosbackend.model.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // Gera a chave de assinatura (funciona com qualquer string, mas deve ter >32 bytes)
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, Role papel) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setSubject(email)
                .claim("papel", papel.name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey())
                .compact();  // sem necessidade de especificar algoritmo, o padrão é HS256
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}