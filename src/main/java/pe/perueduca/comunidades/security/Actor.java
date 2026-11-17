package pe.perueduca.comunidades.security;

import java.util.List;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public record Actor(Long id, String correo, String rol) {
  public List<GrantedAuthority> authorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
  }

  public boolean admin() {
    return rol.equals("ADMIN");
  }

  public boolean estudiante() {
    return rol.equals("ESTUDIANTE");
  }

  public boolean docente() {
    return rol.equals("DOCENTE");
  }

  public static Actor actual() {
    return (Actor) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
