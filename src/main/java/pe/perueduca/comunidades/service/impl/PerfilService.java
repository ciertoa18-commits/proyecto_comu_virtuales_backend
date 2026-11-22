package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.AuditoriaAccion;
import pe.perueduca.comunidades.util.TextPolicy;

import java.time.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;

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
  private final UsuarioIdiomaRepository usuarioIdiomas;
  private final CuentaService cuentas;
  private final CorreoService correo;

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
    cuentas.validarGmailLibre(d.correoRecuperacion(), u.getIdUsuario());
    u.setCorreoRecuperacion(d.correoRecuperacion().trim().toLowerCase(java.util.Locale.ROOT));
    if (d.idiomas() != null) guardarIdiomas(u.getIdUsuario(), d.idiomas());
    if (Actor.actual().docente()) u.setEspecialidad(text.clean(d.especialidad()));
    u.setFechaActualizacion(LocalDateTime.now());
    return u;
  }

  @Transactional(readOnly = true)
  public java.util.List<Long> misIdiomas() {
    return usuarioIdiomas.findByIdUsuario(Actor.actual().id()).stream()
        .map(UsuarioIdioma::getIdIdioma)
        .toList();
  }

  private void guardarIdiomas(Long idUsuario, java.util.List<Long> lista) {
    java.util.Set<Long> unicos = new java.util.LinkedHashSet<>(lista);
    for (Long id : unicos) if (!idiomas.existsById(id)) throw ApiException.rule("Uno de los idiomas no existe");
    usuarioIdiomas.deleteByIdUsuario(idUsuario);
    usuarioIdiomas.flush();
    unicos.forEach(id -> usuarioIdiomas.save(new UsuarioIdioma(idUsuario, id)));
  }

  public java.util.Map<String, Object> enviarCodigoPassword() {
    Usuario u = me();
    if (u.getCorreoRecuperacion() == null || u.getCorreoRecuperacion().isBlank())
      throw ApiException.rule("Registra primero tu Gmail personal en tus datos");
    String codigo = cuentas.nuevoCodigo(u);
    if (codigo == null)
      throw ApiException.rule("Espera un minuto antes de pedir otro código");
    correo.enviar(
        u.getCorreoRecuperacion(),
        "Código para cambiar tu contraseña - Comunidades Virtuales",
        "Hola "
            + u.getNombres()
            + ",\n\nTu código para confirmar el cambio de contraseña es: "
            + codigo
            + "\n\nVence en "
            + CuentaService.minutosVigencia()
            + " minutos y solo se puede usar una vez.\nSi no fuiste tú, ignora este correo y"
            + " cambia tu contraseña.\n\nComunidades Virtuales - PerúEduca");
    return java.util.Map.of(
        "destino", ocultar(u.getCorreoRecuperacion()), "correoActivo", correo.configurado());
  }

  private static String ocultar(String correo) {
    int arroba = correo.indexOf('@');
    if (arroba <= 2) return correo;
    return correo.substring(0, 2) + "*".repeat(arroba - 2) + correo.substring(arroba);
  }

  @Transactional(noRollbackFor = ApiException.class)
  public void password(Requests.Password d) {
    Usuario u = repo.bloquearPorCorreo(Actor.actual().correo()).orElseThrow(ApiException::missing);
    if (u.getPasswordHash() == null || !encoder.matches(d.actual(), u.getPasswordHash()))
      throw ApiException.rule("La contraseña actual no es correcta");
    if (d.nueva().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw ApiException.rule("La contraseña admite hasta 72 bytes UTF-8");
    cuentas.usarCodigo(u, d.codigo(), ApiException.rule("El código no es válido o ya venció"));
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
