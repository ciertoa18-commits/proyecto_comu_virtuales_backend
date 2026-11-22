package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.Rol;
import pe.perueduca.comunidades.entity.Usuario;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.repository.AuditoriaRepository;
import pe.perueduca.comunidades.repository.RolRepository;
import pe.perueduca.comunidades.repository.UsuarioRepository;
import pe.perueduca.comunidades.security.JwtService;

@ExtendWith(MockitoExtension.class)
class LoginConGmailTest {

  @Mock UsuarioRepository usuarios;
  @Mock RolRepository roles;
  @Mock PasswordEncoder encoder;
  @Mock JwtService jwt;
  @Mock AuditoriaRepository auditoria;

  @InjectMocks AuthServiceImpl auth;

  private Usuario usuario(RolCodigo rol) {
    Usuario u = new Usuario();
    u.setIdUsuario(7L);
    u.setIdRol(1L);
    u.setCorreo("ana@perueduca.pe");
    u.setCorreoRecuperacion("ana.quispe@gmail.com");
    u.setPasswordHash("hash");
    u.setIntentosFallidos(0L);
    u.setEstado(UsuarioEstado.ACTIVO);
    Rol r = new Rol();
    r.setIdRol(1L);
    r.setCodigo(rol);
    lenient().when(roles.findById(1L)).thenReturn(Optional.of(r));
    return u;
  }

  @Test
  void administradorDocenteYAlumnoPuedenIngresarConSuGmail() {
    for (RolCodigo rol : RolCodigo.values()) {
      Usuario u = usuario(rol);
      when(usuarios.bloquearPorCorreo("ana.quispe@gmail.com")).thenReturn(Optional.empty());
      when(usuarios.findByCorreoRecuperacion("ana.quispe@gmail.com")).thenReturn(List.of(u));
      when(usuarios.bloquearPorCorreo("ana@perueduca.pe")).thenReturn(Optional.of(u));
      when(encoder.matches("Comunidades2026!", "hash")).thenReturn(true);
      when(jwt.issue("ana@perueduca.pe")).thenReturn("token");

      var r = auth.login(new Requests.Login(" Ana.Quispe@Gmail.com ", "Comunidades2026!"));

      assertTrue(r.ok(), "rol " + rol);
      assertEquals(rol.name(), r.rol());
      assertEquals("token", r.token());
    }
  }

  @Test
  void conGmailYContrasenaIncorrectaNoIngresa() {
    Usuario u = usuario(RolCodigo.ESTUDIANTE);
    when(usuarios.bloquearPorCorreo("ana.quispe@gmail.com")).thenReturn(Optional.empty());
    when(usuarios.findByCorreoRecuperacion("ana.quispe@gmail.com")).thenReturn(List.of(u));
    when(usuarios.bloquearPorCorreo("ana@perueduca.pe")).thenReturn(Optional.of(u));
    when(encoder.matches("otra", "hash")).thenReturn(false);

    assertFalse(auth.login(new Requests.Login("ana.quispe@gmail.com", "otra")).ok());
    assertEquals(1L, u.getIntentosFallidos());
    verify(jwt, never()).issue(anyString());
  }

  @Test
  void unGmailRepetidoEnDosCuentasNoPermiteIngresar() {
    List<Usuario> dos = List.of(usuario(RolCodigo.ESTUDIANTE), usuario(RolCodigo.DOCENTE));
    when(usuarios.bloquearPorCorreo("compartido@gmail.com")).thenReturn(Optional.empty());
    when(usuarios.findByCorreoRecuperacion("compartido@gmail.com")).thenReturn(dos);
    when(encoder.encode(anyString())).thenReturn("x");

    assertFalse(auth.login(new Requests.Login("compartido@gmail.com", "Comunidades2026!")).ok());
    verify(jwt, never()).issue(anyString());
  }
}
