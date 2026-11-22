package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.MiembroComunidadEstado;
import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class NotificationService {
  private final NotificacionRepository repo;
  private final MiembroComunidadRepository miembros;
  private final UsuarioRepository usuarios;
  private final RolRepository roles;

  @Value("${app.db.triggers-activos:false}")
  private boolean triggers;

  public void send(
      Long to, Long community, NotificacionTipo type, String title, String entity, Long id) {
    if (to.equals(Actor.actual().id())) return;
    Notificacion n = new Notificacion();
    n.setIdDestinatario(to);
    n.setIdEmisor(Actor.actual().id());
    n.setIdComunidad(community);
    n.setTipo(type);
    n.setTitulo(title);
    n.setEntidadTipo(entity);
    n.setEntidadId(id);
    n.setLeida(false);
    n.setFechaCreacion(LocalDateTime.now());
    repo.save(n);
  }

  public void broadcast(
      Long community, NotificacionTipo type, String title, String entity, Long id) {
    if (triggers
        && (type == NotificacionTipo.NUEVA_PUBLICACION
            || type == NotificacionTipo.NUEVO_COMENTARIO
            || type == NotificacionTipo.SOLICITUD_INGRESO)) return;
    miembros.findByIdComunidad(community).stream()
        .filter(m -> m.getEstado() == MiembroComunidadEstado.ACTIVO)
        .map(MiembroComunidad::getIdUsuario)
        .distinct()
        .forEach(u -> send(u, community, type, title, entity, id));
  }

  public void solicitud(Long community, Long id) {
    if (triggers) return;
    usuarios.findAll().stream()
        .filter(
            u ->
                u.getEstado() == UsuarioEstado.ACTIVO
                    && roles.findById(u.getIdRol()).orElseThrow().getCodigo() == RolCodigo.ADMIN)
        .forEach(
            u ->
                send(
                    u.getIdUsuario(),
                    community,
                    NotificacionTipo.SOLICITUD_INGRESO,
                    "Nueva solicitud de ingreso",
                    "SOLICITUD",
                    id));
  }

  public void comment(Long to, Long community, Long id) {
    if (!triggers)
      send(to, community, NotificacionTipo.NUEVO_COMENTARIO, "Nuevo comentario", "COMENTARIO", id);
  }

  @Transactional(readOnly = true)
  public long sinLeer() {
    return repo.countByIdDestinatarioAndLeida(Actor.actual().id(), false);
  }

  public org.springframework.data.domain.Page<Notificacion> list(int page, int size) {
    return repo.findAll(
        (r, q, b) -> b.equal(r.get("idDestinatario"), Actor.actual().id()),
        org.springframework.data.domain.PageRequest.of(
            Math.max(0, page),
            Math.max(1, Math.min(size, 100)),
            org.springframework.data.domain.Sort.by("fechaCreacion").descending()));
  }

  public void read(Long id) {
    Notificacion n = propia(id);
    n.setLeida(true);
    if (n.getFechaLectura() == null) n.setFechaLectura(LocalDateTime.now());
  }

  public void unread(Long id) {
    Notificacion n = propia(id);
    n.setLeida(false);
    n.setFechaLectura(null);
  }

  public void delete(Long id) {
    repo.delete(propia(id));
  }

  /** Solo el destinatario puede leer, marcar o eliminar su notificación. */
  private Notificacion propia(Long id) {
    Notificacion n = repo.findById(id).orElseThrow(ApiException::missing);
    if (!n.getIdDestinatario().equals(Actor.actual().id())) throw ApiException.denied();
    return n;
  }

  public void readAll() {
    repo.findByIdDestinatario(Actor.actual().id())
        .forEach(
            n -> {
              n.setLeida(true);
              if (n.getFechaLectura() == null) n.setFechaLectura(LocalDateTime.now());
            });
  }
}
