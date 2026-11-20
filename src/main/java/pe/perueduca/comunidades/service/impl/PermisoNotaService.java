package pe.perueduca.comunidades.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;
import pe.perueduca.comunidades.util.TextPolicy;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class PermisoNotaService {
  static final String PENDIENTE = "PENDIENTE";
  static final String APROBADO = "APROBADO";
  static final String RECHAZADO = "RECHAZADO";
  private static final int HORAS_VIGENCIA = 24;

  private final PermisoNotaRepository permisos;
  private final SeccionRepository secciones;
  private final CursoRepository cursos;
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final AccessService access;
  private final NotificationService notificaciones;
  private final CorreoService correo;
  private final TextPolicy text;

  public record VistaPermiso(
      Long idPermiso,
      Long idSeccion,
      String curso,
      String grupo,
      Long idDocente,
      String docente,
      String motivo,
      String estado,
      LocalDateTime fechaSolicitud,
      String respuesta,
      LocalDateTime fechaRespuesta,
      LocalDateTime vence,
      boolean vigente) {}

  public VistaPermiso solicitar(Long idSeccion, Requests.PermisoNotaDatos d) {
    access.teacher(idSeccion);
    Long yo = Actor.actual().id();
    Optional<PermisoNota> ultimo = ultimo(idSeccion, yo);
    if (ultimo.isPresent() && PENDIENTE.equals(ultimo.get().getEstado()))
      throw ApiException.conflict("Ya tienes una solicitud pendiente para esta sección");
    PermisoNota p = new PermisoNota();
    p.setIdSeccion(idSeccion);
    p.setIdDocente(yo);
    p.setMotivo(text.required(d.motivo()));
    p.setEstado(PENDIENTE);
    p.setFechaSolicitud(LocalDateTime.now());
    p = permisos.save(p);
    VistaPermiso v = vista(p);
    Long idAdmin = idRol(RolCodigo.ADMIN);
    for (Usuario admin : usuarios.findByIdRol(idAdmin)) {
      if (admin.getEstado() != UsuarioEstado.ACTIVO) continue;
      notificaciones.send(
          admin.getIdUsuario(),
          null,
          NotificacionTipo.SISTEMA,
          "Permiso para editar notas: " + v.docente() + " (" + v.curso() + ")",
          "PERMISO_NOTA",
          p.getIdPermiso());
      correo.enviar(
          destino(admin),
          "Solicitud de permiso para editar notas - Comunidades Virtuales",
          v.docente()
              + " solicita permiso para modificar notas de "
              + v.curso()
              + " (grupo "
              + v.grupo()
              + ").\n\nMotivo: "
              + p.getMotivo()
              + "\n\nRevisa la solicitud en Administración > Permisos de notas.");
    }
    return v;
  }

  @Transactional(readOnly = true)
  public Optional<VistaPermiso> estado(Long idSeccion) {
    access.teacher(idSeccion);
    return ultimo(idSeccion, Actor.actual().id()).map(this::vista);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional(readOnly = true)
  public List<VistaPermiso> listar(String estado) {
    return permisos.findByEstadoOrderByFechaSolicitudDesc(estado).stream().map(this::vista).toList();
  }

  @PreAuthorize("hasRole('ADMIN')")
  public VistaPermiso resolver(Long id, Requests.RespuestaPermiso d) {
    PermisoNota p = permisos.findById(id).orElseThrow(ApiException::missing);
    if (!PENDIENTE.equals(p.getEstado())) return vista(p);
    LocalDateTime ahora = LocalDateTime.now();
    p.setEstado(d.aprobar() ? APROBADO : RECHAZADO);
    p.setIdRevisor(Actor.actual().id());
    p.setRespuesta(text.clean(d.respuesta()));
    p.setFechaRespuesta(ahora);
    p.setVence(d.aprobar() ? ahora.plusHours(HORAS_VIGENCIA) : null);
    VistaPermiso v = vista(p);
    String titulo =
        d.aprobar()
            ? "Puedes modificar las notas de " + v.curso() + " durante " + HORAS_VIGENCIA + " horas"
            : "No se aprobó modificar las notas de " + v.curso();
    notificaciones.send(p.getIdDocente(), null, NotificacionTipo.SISTEMA, titulo, "PERMISO_NOTA", id);
    usuarios
        .findById(p.getIdDocente())
        .ifPresent(
            u ->
                correo.enviar(
                    destino(u),
                    "Respuesta a tu solicitud para editar notas - Comunidades Virtuales",
                    titulo
                        + "."
                        + (p.getRespuesta() == null ? "" : "\n\nRespuesta: " + p.getRespuesta())));
    return v;
  }

  @Transactional(readOnly = true)
  public boolean tienePermiso(Long idSeccion, Long idDocente) {
    LocalDateTime ahora = LocalDateTime.now();
    return permisos.findByIdSeccionAndIdDocenteOrderByFechaSolicitudDesc(idSeccion, idDocente).stream()
        .anyMatch(p -> APROBADO.equals(p.getEstado()) && p.getVence() != null && p.getVence().isAfter(ahora));
  }

  private Optional<PermisoNota> ultimo(Long idSeccion, Long idDocente) {
    return permisos.findByIdSeccionAndIdDocenteOrderByFechaSolicitudDesc(idSeccion, idDocente).stream()
        .findFirst();
  }

  private VistaPermiso vista(PermisoNota p) {
    Seccion s = secciones.findById(p.getIdSeccion()).orElse(null);
    String curso =
        s == null ? "" : cursos.findById(s.getIdCurso()).map(Curso::getNombre).orElse("");
    String docente =
        usuarios
            .findById(p.getIdDocente())
            .map(u -> u.getNombres() + " " + u.getApellidos())
            .orElse("");
    boolean vigente =
        APROBADO.equals(p.getEstado())
            && p.getVence() != null
            && p.getVence().isAfter(LocalDateTime.now());
    return new VistaPermiso(
        p.getIdPermiso(),
        p.getIdSeccion(),
        curso,
        s == null ? "" : s.getGrupo(),
        p.getIdDocente(),
        docente,
        p.getMotivo(),
        p.getEstado(),
        p.getFechaSolicitud(),
        p.getRespuesta(),
        p.getFechaRespuesta(),
        p.getVence(),
        vigente);
  }

  private Long idRol(RolCodigo codigo) {
    return roles.findAll().stream()
        .filter(r -> r.getCodigo() == codigo)
        .findFirst()
        .orElseThrow(ApiException::missing)
        .getIdRol();
  }

  private static String destino(Usuario u) {
    return u.getCorreoRecuperacion() != null && !u.getCorreoRecuperacion().isBlank()
        ? u.getCorreoRecuperacion()
        : u.getCorreo();
  }
}
