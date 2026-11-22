package com.projectapex.config;

import com.projectapex.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                Claims claims = jwtService.parseToken(token);
                String uid = claims.getSubject();
                String rawRole = claims.get("role", String.class);

                if (uid != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String authority = "ROLE_PLAYER";
                    if (rawRole != null) {
                        String clean = rawRole.trim().toUpperCase();
                        authority = clean.startsWith("ROLE_") ? clean : "ROLE_" + clean;
                    }
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(uid, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Invalid token; proceed as anonymous
            }
        }

        filterChain.doFilter(request, response);
    }
}
