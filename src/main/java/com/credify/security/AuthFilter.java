package com.credify.security;

import com.credify.model.User;
import com.credify.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public AuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method) || isPublicRequest(path, method)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Bearer token required");
            return;
        }

        try {
            String token = header.substring(7).trim();
            String userId = jwt.userId(token);
            String tokenRole = jwt.role(token);
            User user = users.findById(userId).orElse(null);
            if (user == null || !"ACTIVE".equals(user.accountStatus)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Account is unavailable");
                return;
            }
            // Read the current role from MongoDB so role changes take effect immediately.
            String role = user.role;
            if (tokenRole == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);

            if (path.startsWith("/api/admin") && !"ADMIN".equals(role)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required");
                return;
            }
        } catch (Exception exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicRequest(String path, String method) {
        if (path.startsWith("/api/auth/")) return true;
        if ("GET".equalsIgnoreCase(method) && "/api/health".equals(path)) return true;
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/reviews")) return true;
        return "GET".equalsIgnoreCase(method)
                && path.startsWith("/api/users/")
                && !path.startsWith("/api/users/me/");
    }
}