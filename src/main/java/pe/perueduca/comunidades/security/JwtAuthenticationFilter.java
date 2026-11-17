package pe.perueduca.comunidades.security;

import pe.perueduca.comunidades.security.JwtService;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.perueduca.comunidades.service.impl.AuthServiceImpl;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwt;
  private final AuthServiceImpl auth;

  public JwtAuthenticationFilter(JwtService jwt, AuthServiceImpl auth) {
    this.jwt = jwt;
    this.auth = auth;
  }

  private static final java.util.Set<String> PUBLICAS =
      java.util.Set.of(
          "/api/v1/auth/login",
          "/api/v1/auth/registro",
          "/api/v1/auth/recuperacion",
          "/api/v1/auth/restablecer");

  protected boolean shouldNotFilter(HttpServletRequest q) {
    return PUBLICAS.contains(q.getServletPath()) || q.getMethod().equals("OPTIONS");
  }

  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, IOException {
    String header = q.getHeader("Authorization");
    if (header != null) {
      try {
        if (!header.startsWith("Bearer ")) throw new IllegalArgumentException();
        String token = header.substring(7);
        var claims = jwt.parse(token);
        auth.validateSession(token, claims);
        var actor = auth.actor(claims.getSubject());
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, null, actor.authorities()));
      } catch (io.jsonwebtoken.JwtException
          | IllegalArgumentException
          | org.springframework.security.core.AuthenticationException e) {
        SecurityContextHolder.clearContext();
        r.setStatus(401);
        r.setContentType("application/json");
        r.getWriter()
            .write("{\"codigo\":\"TOKEN_INVALIDO\",\"mensaje\":\"Token inválido o vencido\"}");
        return;
      }
    }
    chain.doFilter(q, r);
  }
}
