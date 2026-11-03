package pe.edu.upeu.comunidades.service.impl;

import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class ComunidadServiceImpl implements pe.edu.upeu.comunidades.service.ComunidadService {
  private final ComunidadRepository repo;
  private final MiembroComunidadRepository miembros;
  private final SolicitudIngresoRepository solicitudes;
  private final AccessService access;
  private final NotificationService notifications;
  private final TextPolicy text;
  private final CategoriaRepository categorias;
  private final IdiomaRepository idiomas;
  private final PuebloOriginarioRepository pueblos;

  @Transactional(readOnly = true)
  public Page<Comunidad> buscar(String q, Long cat, Long lang, int page, int size) {
    return repo.findAll(
        (r, query, b) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          p.add(b.equal(r.get("estado"), ComunidadEstado.ACTIVA));
          if (q != null && !q.isBlank())
            p.add(
                b.like(
                    b.lower(r.get("nombre")),
                    "%" + q.toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%",
                    '\\'));
          if (cat != null) p.add(b.equal(r.get("idCategoria"), cat));
          if (lang != null) p.add(b.equal(r.get("idIdioma"), lang));
          return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },
        PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("nombre")));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Comunidad guardar(Long id, Requests.ComunidadDatos d) {
    if (!categorias.existsById(d.idCategoria())
        || d.idIdioma() != null && !idiomas.existsById(d.idIdioma())
        || d.idPueblo() != null && !pueblos.existsById(d.idPueblo())) throw ApiException.missing();
    if (d.idImagen() != null) {
      Archivo f = access.ownFile(d.idImagen());
      if (!Set.of("jpg", "jpeg", "png").contains(f.getExtension()))
        throw ApiException.rule("La imagen debe ser JPG o PNG");
    }
    Comunidad c = id == null ? new Comunidad() : access.comunidad(id);
    c.setNombre(text.required(d.nombre()));
    c.setDescripcion(text.clean(d.descripcion()));
    c.setIdCategoria(d.idCategoria());
    c.setIdIdioma(d.idIdioma());
    c.setIdPueblo(d.idPueblo());
    c.setVisibilidad(d.visibilidad());
    c.setModeracionPrevia(d.moderacionPrevia());
    c.setIdImagen(d.idImagen());
    if (id == null) {
      c.setIdCreador(Actor.actual().id());
      c.setEstado(ComunidadEstado.ACTIVA);
      c.setFechaCreacion(LocalDateTime.now());
    } else c.setFechaActualizacion(LocalDateTime.now());
    return repo.save(c);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void desactivar(Long id) {
    access.comunidad(id).setEstado(ComunidadEstado.INACTIVA);
  }

  private Comunidad locked(Long id) {
    Comunidad c = repo.bloquear(id).orElseThrow(ApiException::missing);
    if (c.getEstado() != ComunidadEstado.ACTIVA) throw ApiException.missing();
    return c;
  }

  private MiembroComunidad activate(Long community, Long user) {
    MiembroComunidad m =
        miembros.findByIdComunidad(community).stream()
            .filter(x -> x.getIdUsuario().equals(user))
            .findFirst()
            .orElseGet(MiembroComunidad::new);
    if (m.getEstado() == MiembroComunidadEstado.EXPULSADO) throw ApiException.denied();
    if (m.getEstado() == MiembroComunidadEstado.ACTIVO)
      throw ApiException.conflict("Ya pertenece a la comunidad");
    m.setIdComunidad(community);
    m.setIdUsuario(user);
    m.setRolComunidad(MiembroComunidadRolComunidad.MIEMBRO);
    m.setEstado(MiembroComunidadEstado.ACTIVO);
    m.setFechaIngreso(LocalDateTime.now());
    m.setFechaSalida(null);
    return miembros.save(m);
  }

  @PreAuthorize("hasAnyRole('ESTUDIANTE','DOCENTE')")
  public MiembroComunidad unirse(Long id) {
    if (locked(id).getVisibilidad() != ComunidadVisibilidad.PUBLICA)
      throw ApiException.rule("La comunidad privada requiere una solicitud aprobada");
    return activate(id, Actor.actual().id());
  }

  @PreAuthorize("hasAnyRole('ESTUDIANTE','DOCENTE')")
  public SolicitudIngreso solicitar(Long id, Requests.Solicitud d) {
    if (locked(id).getVisibilidad() != ComunidadVisibilidad.PRIVADA)
      throw ApiException.rule("La comunidad pública admite ingreso directo");
    if (miembros.findByIdComunidad(id).stream()
        .anyMatch(
            m ->
                m.getIdUsuario().equals(Actor.actual().id())
                    && (m.getEstado() == MiembroComunidadEstado.ACTIVO
                        || m.getEstado() == MiembroComunidadEstado.EXPULSADO)))
      throw ApiException.conflict("No puede solicitar ingreso en su estado actual");
    if (solicitudes.findByIdComunidad(id).stream()
        .anyMatch(
            s ->
                s.getIdUsuario().equals(Actor.actual().id())
                    && s.getEstado() == SolicitudIngresoEstado.PENDIENTE))
      throw ApiException.conflict("Ya existe una solicitud pendiente");
    SolicitudIngreso s = new SolicitudIngreso();
    s.setIdComunidad(id);
    s.setIdUsuario(Actor.actual().id());
    s.setMensaje(text.clean(d.mensaje()));
    s.setEstado(SolicitudIngresoEstado.PENDIENTE);
    s.setFechaSolicitud(LocalDateTime.now());
    s = solicitudes.save(s);
    notifications.solicitud(id, s.getIdSolicitud());
    return s;
  }

  public void salir(Long id) {
    locked(id);
    MiembroComunidad m = access.membership(id);
    if (m.getRolComunidad() == MiembroComunidadRolComunidad.ADMINISTRADOR
        && miembros.findByIdComunidad(id).stream()
                .filter(
                    x ->
                        x.getRolComunidad() == MiembroComunidadRolComunidad.ADMINISTRADOR
                            && x.getEstado() == MiembroComunidadEstado.ACTIVO)
                .count()
            == 1)
      throw ApiException.conflict("Asigne otro administrador antes de abandonar la comunidad");
    m.setEstado(MiembroComunidadEstado.RETIRADO);
    m.setFechaSalida(LocalDateTime.now());
  }

  public void expulsar(Long id, Long user) {
    locked(id);
    access.moderator(id);
    if (user.equals(Actor.actual().id()))
      throw ApiException.rule("Use abandonar comunidad para retirarse");
    MiembroComunidad m =
        miembros.findByIdComunidad(id).stream()
            .filter(
                x ->
                    x.getIdUsuario().equals(user) && x.getEstado() == MiembroComunidadEstado.ACTIVO)
            .findFirst()
            .orElseThrow(ApiException::missing);
    if (!Actor.actual().admin() && m.getRolComunidad() != MiembroComunidadRolComunidad.MIEMBRO)
      throw ApiException.denied();
    m.setEstado(MiembroComunidadEstado.EXPULSADO);
    m.setFechaSalida(LocalDateTime.now());
  }

  @PreAuthorize("hasRole('ADMIN')")
  public SolicitudIngreso resolver(Long id, Requests.Resolucion d) {
    SolicitudIngreso s = solicitudes.bloquear(id).orElseThrow(ApiException::missing);
    locked(s.getIdComunidad());
    if (s.getEstado() != SolicitudIngresoEstado.PENDIENTE)
      throw ApiException.conflict("La solicitud ya fue resuelta");
    if (!d.aprobar() && text.clean(d.motivo()) == null)
      throw ApiException.rule("Indique el motivo de rechazo");
    if (d.aprobar()) activate(s.getIdComunidad(), s.getIdUsuario());
    s.setEstado(d.aprobar() ? SolicitudIngresoEstado.APROBADA : SolicitudIngresoEstado.RECHAZADA);
    s.setIdRevisor(Actor.actual().id());
    s.setFechaRespuesta(LocalDateTime.now());
    s.setMotivoRechazo(d.aprobar() ? null : text.clean(d.motivo()));
    notifications.send(
        s.getIdUsuario(),
        s.getIdComunidad(),
        NotificacionTipo.SOLICITUD_RESPUESTA,
        "Solicitud " + s.getEstado(),
        "SOLICITUD",
        id);
    return s;
  }
}
