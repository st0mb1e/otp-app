package ru.yartsev_vladislav.otp_app.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.yartsev_vladislav.otp_app.exception.UnauthorizedException;

@Component
public class RequestScopedCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser require() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new UnauthorizedException("No active request");
        }
        HttpServletRequest request = attrs.getRequest();
        Object value = request.getAttribute(AuthAttributes.CURRENT_USER);
        if (!(value instanceof CurrentUser user)) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
