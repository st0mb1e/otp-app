package ru.yartsev_vladislav.otp_app.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JjwtParser implements JwtParser {

    private final io.jsonwebtoken.JwtParser parser;
    private final String expectedIssuer;

    public JjwtParser(JwtProperties props) {
        SecretKey key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.expectedIssuer = props.issuer();
        this.parser = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.issuer())
                .build();
    }

    @Override
    public JwtClaims parse(String token) {
        try {
            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();

            long userId = Long.parseLong(claims.getSubject());
            String login = claims.get("login", String.class);
            String roleRaw = claims.get("role", String.class);
            Role role = Role.valueOf(roleRaw);

            return new JwtClaims(
                    userId,
                    login,
                    role,
                    claims.getExpiration().toInstant()
            );
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid or expired JWT", ex);
        }
    }

    public String getExpectedIssuer() {
        return expectedIssuer;
    }
}
