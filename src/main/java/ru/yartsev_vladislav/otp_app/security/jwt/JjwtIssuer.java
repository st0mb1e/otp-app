package ru.yartsev_vladislav.otp_app.security.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import ru.yartsev_vladislav.otp_app.domain.Role;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Component
public class JjwtIssuer implements JwtIssuer {

    private final SecretKey signingKey;
    private final JwtProperties props;

    public JjwtIssuer(SecretKey jwtSigningKey, JwtProperties props) {
        this.signingKey = jwtSigningKey;
        this.props = props;
    }

    @Override
    public IssuedToken issue(long userId, String login, Role role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(props.ttl());
        String token = Jwts.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(userId))
                .claim("login", login)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, expiresAt);
    }
}
