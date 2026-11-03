package pe.edu.upeu.comunidades.service.impl;

import java.time.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class PerfilService {
  private final UsuarioRepository repo;
  private final IdiomaRepository idiomas;
  private final PasswordEncoder encoder;
  private final TextPolicy text;
  private final AccessService access;
  private final AuditService audit;

  public Usuario me() {
    return repo.findById(Actor.actual().id()).orElseThrow(ApiException::missing);
  }

  public Usuario update(Requests.Perfil d) {
    Usuario u = me();
    if (d.idIdiomaPref() != null && !idiomas.existsById(d.idIdiomaPref()))
      throw ApiException.missing();
    u.setNombres(text.required(d.nombres()));
    u.setApellidos(text.required(d.apellidos()));
    u.setIdIdiomaPref(d.idIdiomaPref());
    u.setTelefono(d.telefono() == null || d.telefono().isBlank() ? null : d.telefono().trim());
    if (Actor.actual().docente()) u.setEspecialidad(text.clean(d.especialidad()));
    u.setFechaActualizacion(LocalDateTime.now());
    return u;
  }

  public void password(Requests.Password d) {
    Usuario u = repo.bloquearPorCorreo(Actor.actual().correo()).orElseThrow(ApiException::missing);
    if (u.getPasswordHash() == null || !encoder.matches(d.actual(), u.getPasswordHash()))
      throw ApiException.rule("La contraseña actual no es correcta");
    if (d.nueva().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.rule("La contraseña admite hasta 72 bytes UTF-8");
    u.setPasswordHash(encoder.encode(d.nueva()));
    u.setFechaActualizacion(LocalDateTime.now());
    audit.record(
        "USUARIO", u.getIdUsuario(), AuditoriaAccion.UPDATE, null, "{\"passwordCambiado\":true}");
  }

  public Usuario foto(Long id) {
    Archivo f = access.ownFile(id);
    if (!java.util.Set.of("jpg", "jpeg", "png").contains(f.getExtension()))
      throw ApiException.rule("La foto debe ser JPG o PNG");
    Usuario u = me();
    u.setIdFotoPerfil(id);
    u.setFechaActualizacion(LocalDateTime.now());
    return u;
  }
}
