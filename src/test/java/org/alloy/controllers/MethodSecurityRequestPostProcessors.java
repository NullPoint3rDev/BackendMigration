package org.alloy.controllers;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * В {@code @AlloyWebMvcTest} security-фильтры отключены ({@code addFilters=false}), поэтому штатный
 * {@code SecurityMockMvcRequestPostProcessors.user(...)} не доносит SecurityContext до method security
 * (нет {@code SecurityContextPersistenceFilter}) — {@code @PreAuthorize} видит "нет аутентификации" и отдаёт 401.
 *
 * Этот RPP кладёт {@link UsernamePasswordAuthenticationToken} прямо в {@link SecurityContextHolder} того же
 * потока, в котором MockMvc синхронно вызывает контроллер, поэтому {@code @PreAuthorize} получает нужного
 * пользователя. Контекст обязательно чистить в {@code @AfterEach} (метод {@link #clear()}).
 */
final class MethodSecurityRequestPostProcessors {

    private MethodSecurityRequestPostProcessors() {
    }

    static RequestPostProcessor authn(UserDetails user) {
        return request -> {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                    user, user.getPassword(), user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(token);
            return request;
        };
    }

    static void clear() {
        SecurityContextHolder.clearContext();
    }
}
