package pe.edu.upeu.comunidades.service.impl;

import java.time.*;
import java.util.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.comunidades.config.JwtService;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
public class AuthServiceImpl implements pe.edu.upeu.comunidades.service.AuthService {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  @com.fasterxml.jackson.annotation.JsonInclude(
      com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
  public record LoginResult(boolean ok, String token, Long idUsuario, String rol, String motivo) {
    LoginResult(boolean ok, String token, Long idUsuario, String rol) {
      this(ok, token, idUsuario, rol, null);
    }

    static LoginResult rechazo(String motivo) {
      return new LoginResult(false, null, null, null, motivo);
    }
  }

  // A failed login returns normally so the increment/lock is committed before HTTP 401.
  @Transactional
  public LoginResult login(Requests.Login req) {
    String correo = req.correo().trim().toLowerCase(Locale.ROOT);
    var optional = usuarios.bloquearPorCorreo(correo);
    if (optional.isEmpty()) {
      encoder.matches(
          req.password(), "$2a$10$7EqJtq98hPqEX7fNZaFWoO5H7jGzL7DFzF5uAQ3QK8WzWsPmgxZ9i");
      return new LoginResult(false, null, null, null);
    }
    Usuario u = optional.get();
    LocalDateTime now = LocalDateTime.now();
    if (u.getEstado() == UsuarioEstado.PENDIENTE)
      return u.getPasswordHash() != null && encoder.matches(req.password(), u.getPasswordHash())
          ? LoginResult.rechazo("CUENTA_PENDIENTE")
          : new LoginResult(false, null, null, null);
    if (u.getEstado() != UsuarioEstado.ACTIVO) return new LoginResult(false, null, null, null);
    if (u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(now))
      return LoginResult.rechazo("CUENTA_BLOQUEADA");
    if (u.getBloqueadoHasta() != null) {
      u.setIntentosFallidos(0L);
      u.setBloqueadoHasta(null);
    }
    if (u.getPasswordHash() == null || !encoder.matches(req.password(), u.getPasswordHash())) {
      long n = u.getIntentosFallidos() + 1;
      u.setIntentosFallidos(n);
      if (n >= 5) {
        u.setBloqueadoHasta(now.plusMinutes(15));
        return LoginResult.rechazo("CUENTA_BLOQUEADA");
      }
      return new LoginResult(false, null, null, null);
    }
    u.setIntentosFallidos(0L);
    u.setBloqueadoHasta(null);
    return new LoginResult(
        true,
        jwt.issue(correo),
        u.getIdUsuario(),
        roles.findById(u.getIdRol()).orElseThrow().getCodigo().name());
  }

  @Transactional(readOnly = true)
  public Actor actor(String correo) {
    Usuario u =
        usuarios
            .findByCorreo(correo)
            .orElseThrow(() -> new BadCredentialsException("Usuario no disponible"));
    if (u.getEstado() != UsuarioEstado.ACTIVO
        || u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now()))
      throw new BadCredentialsException("Usuario no disponible");
    return new Actor(
        u.getIdUsuario(),
        u.getCorreo(),
        roles.findById(u.getIdRol()).orElseThrow().getCodigo().name());
  }

  @Transactional
  public void logout(String token) {
    var c = jwt.parse(token);
    Auditoria a = new Auditoria();
    a.setTabla("SESION_JWT");
    a.setIdRegistro(actor(c.getSubject()).id());
    a.setAccion(AuditoriaAccion.DELETE);
    a.setIdUsuario(a.getIdRegistro());
    a.setFecha(LocalDateTime.now());
    a.setValoresDespues("{\"jti\":\"" + c.getId() + "\"}");
    auditoria.save(a);
  }

  private final AuditoriaRepository auditoria;

  @Transactional(readOnly = true)
  public void validateSession(String token, io.jsonwebtoken.Claims c) {
    boolean revoked =
        auditoria.findByIdUsuario(actor(c.getSubject()).id()).stream()
            .anyMatch(
                a ->
                    "SESION_JWT".equals(a.getTabla())
                        && a.getValoresDespues() != null
                        && a.getValoresDespues().contains(c.getId()));
    if (revoked) throw new BadCredentialsException("Sesión cerrada");
  }
}
