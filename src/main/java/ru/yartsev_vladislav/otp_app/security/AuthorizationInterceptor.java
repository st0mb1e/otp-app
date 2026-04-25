package ru.yartsev_vladislav.otp_app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.yartsev_vladislav.otp_app.domain.Role;
import ru.yartsev_vladislav.otp_app.exception.ForbiddenException;
import ru.yartsev_vladislav.otp_app.exception.UnauthorizedException;

import java.util.Arrays;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresAuth requirement = resolveRequirement(handlerMethod);
        if (requirement == null) {
            return true;
        }

        CurrentUser user = (CurrentUser) request.getAttribute(AuthAttributes.CURRENT_USER);
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }

        Role[] allowed = requirement.roles();
        if (allowed.length > 0 && Arrays.stream(allowed).noneMatch(r -> r == user.role())) {
            throw new ForbiddenException("Access denied: requires one of " + Arrays.toString(allowed));
        }

        return true;
    }

    private RequiresAuth resolveRequirement(HandlerMethod handlerMethod) {
        RequiresAuth onMethod = handlerMethod.getMethodAnnotation(RequiresAuth.class);
        if (onMethod != null) {
            return onMethod;
        }
        return handlerMethod.getBeanType().getAnnotation(RequiresAuth.class);
    }
}
