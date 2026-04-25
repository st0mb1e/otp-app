package ru.yartsev_vladislav.otp_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.yartsev_vladislav.otp_app.security.jwt.JwtClaims;
import ru.yartsev_vladislav.otp_app.security.jwt.JwtParser;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtParser jwtParser;

    public JwtAuthenticationFilter(JwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                try {
                    JwtClaims claims = jwtParser.parse(token);
                    CurrentUser user = new CurrentUser(claims.userId(), claims.login(), claims.role());
                    request.setAttribute(AuthAttributes.CURRENT_USER, user);
                } catch (JwtParser.InvalidTokenException ex) {
                    log.debug("Rejected invalid JWT: {}", ex.getMessage());
                    // do not set the attribute; protected endpoints will respond 401 via interceptor
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
