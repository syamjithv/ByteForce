package com.byteforce.web.interceptor;

import com.byteforce.domain.Role;
import com.byteforce.domain.User;
import com.byteforce.web.util.WebSessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Backend security interceptor that guards all /admin/** routes.
 * 1. Unauthenticated users are redirected to /login with returnUrl.
 * 2. Authenticated users with Role.STUDENT are denied access with HTTP 403 Forbidden.
 * 3. Authenticated users with Role.ADMIN are granted access.
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Optional<User> userOpt = WebSessionUtil.getCurrentUser(session);

        if (userOpt.isEmpty()) {
            String targetUri = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isBlank()) {
                targetUri += "?" + queryString;
            }
            String encodedReturnUrl = URLEncoder.encode(targetUri, StandardCharsets.UTF_8);
            response.sendRedirect(request.getContextPath() + "/login?returnUrl=" + encodedReturnUrl);
            return false;
        }

        User user = userOpt.get();
        if (user.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Administrator role required to access administrative console.");
            return false;
        }

        return true;
    }
}
