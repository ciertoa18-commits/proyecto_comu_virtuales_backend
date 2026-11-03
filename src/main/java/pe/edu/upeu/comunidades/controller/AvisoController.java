package pe.edu.upeu.comunidades.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.dto.CuentaRequests;
import pe.edu.upeu.comunidades.service.impl.*;

/** Enviar avisos (notificaciones) y datos de apoyo para elegir destinatarios. */
@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class AvisoController {
  private final AvisoService avisos;
  private final AdminUsuarioService usuarios;
  private final pe.edu.upeu.comunidades.repository.UsuarioRepository usuarioRepo;
  private final pe.edu.upeu.comunidades.repository.RolRepository roles;
  private final pe.edu.upeu.comunidades.repository.MiembroComunidadRepository miembros;
  private final pe.edu.upeu.comunidades.repository.SolicitudIngresoRepository solicitudes;

  @PostMapping("/notificaciones")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
  public AvisoService.Envio enviar(@jakarta.validation.Valid @RequestBody CuentaRequests.Aviso d) {
    return avisos.enviar(d);
  }

  @GetMapping("/usuarios/buscar")
  @PreAuthorize("hasAnyRole('ADMIN','DOCENTE')")
  public Object buscar(@RequestParam String q) {
    return usuarios.buscar(q);
  }

  @GetMapping("/me/secciones")
  public Object misSecciones() {
    return usuarios.misSecciones();
  }

  /** Datos públicos de una persona (para mostrar nombres en mensajes, muro, etc.). */
  @GetMapping("/usuarios/{id}/publico")
  public Object publico(@PathVariable Long id) {
    var u =
        usuarioRepo
            .findById(id)
            .orElseThrow(pe.edu.upeu.comunidades.exception.ApiException::missing);
    var m = new java.util.LinkedHashMap<String, Object>();
    m.put("idUsuario", u.getIdUsuario());
    m.put("nombres", u.getNombres());
    m.put("apellidos", u.getApellidos());
    m.put("rol", roles.findById(u.getIdRol()).map(r -> r.getCodigo().name()).orElse(""));
    m.put("especialidad", u.getEspecialidad());
    m.put("idFotoPerfil", u.getIdFotoPerfil());
    return m;
  }

  /** Comunidades del usuario actual: membresías activas y solicitudes pendientes. */
  @GetMapping("/me/comunidades")
  public Object misComunidades() {
    Long yo = pe.edu.upeu.comunidades.security.Actor.actual().id();
    var lista = new java.util.ArrayList<java.util.Map<String, Object>>();
    miembros.findByIdUsuario(yo).stream()
        .filter(
            m -> m.getEstado() == pe.edu.upeu.comunidades.entity.MiembroComunidadEstado.ACTIVO)
        .forEach(
            m ->
                lista.add(
                    java.util.Map.of(
                        "idComunidad", m.getIdComunidad(),
                        "estado", "MIEMBRO",
                        "rolComunidad", m.getRolComunidad().name())));
    solicitudes.findByIdUsuario(yo).stream()
        .filter(
            s -> s.getEstado() == pe.edu.upeu.comunidades.entity.SolicitudIngresoEstado.PENDIENTE)
        .forEach(
            s ->
                lista.add(
                    java.util.Map.of(
                        "idComunidad", s.getIdComunidad(),
                        "estado", "SOLICITUD_PENDIENTE",
                        "rolComunidad", "")));
    return lista;
  }

  /**
   * Personas a las que puedo escribir un mensaje: las que comparten alguna comunidad conmigo. No
   * expone correos. El administrador puede escribir a cualquier usuario activo.
   */
  @GetMapping("/me/contactos")
  public Object contactos() {
    var actor = pe.edu.upeu.comunidades.security.Actor.actual();
    java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
    if (actor.admin()) {
      usuarioRepo.findAll().forEach(u -> ids.add(u.getIdUsuario()));
    } else {
      miembros.findByIdUsuario(actor.id()).stream()
          .filter(m -> m.getEstado() == pe.edu.upeu.comunidades.entity.MiembroComunidadEstado.ACTIVO)
          .forEach(
              m ->
                  miembros.findByIdComunidad(m.getIdComunidad()).stream()
                      .filter(
                          x ->
                              x.getEstado()
                                  == pe.edu.upeu.comunidades.entity.MiembroComunidadEstado.ACTIVO)
                      .forEach(x -> ids.add(x.getIdUsuario())));
    }
    ids.remove(actor.id());
    return usuarioRepo.findAllById(ids).stream()
        .filter(u -> u.getEstado() == pe.edu.upeu.comunidades.entity.UsuarioEstado.ACTIVO)
        .sorted(java.util.Comparator.comparing(pe.edu.upeu.comunidades.entity.Usuario::getApellidos))
        .map(
            u -> {
              var m = new java.util.LinkedHashMap<String, Object>();
              m.put("idUsuario", u.getIdUsuario());
              m.put("nombres", u.getNombres());
              m.put("apellidos", u.getApellidos());
              m.put("rol", roles.findById(u.getIdRol()).map(r -> r.getCodigo().name()).orElse(""));
              return m;
            })
        .toList();
  }
}
