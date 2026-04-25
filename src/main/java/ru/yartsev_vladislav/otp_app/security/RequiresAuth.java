package ru.yartsev_vladislav.otp_app.security;

import ru.yartsev_vladislav.otp_app.domain.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresAuth {

    Role[] roles() default {};
}
