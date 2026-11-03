package pe.edu.upeu.comunidades.service.impl;

import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class PublicacionServiceImpl implements pe.edu.upeu.comunidades.service.PublicacionService {
  private final PublicacionRepository repo;
  private final ComentarioRepository comentarios;
  private final PublicacionArchivoRepository adjuntos;
  private final AccessService access;
  private final NotificationService notifications;
  private final TextPolicy text;
  private final AuditService audit;
  private final SeccionRepository secciones;
  private final CursoRepository cursos;

  public Publicacion visible(Long id) {
    Publicacion p = repo.findById(id).orElseThrow(ApiException::missing);
    access.member(p.getIdComunidad());
    if (p.getEstado() != PublicacionEstado.APROBADA && !p.getIdAutor().equals(Actor.actual().id()))
      access.moderator(p.getIdComunidad());
    return p;
  }

  // Todos los miembros comparten en el muro (estudiantes incluidos); la moderación previa
  // de la comunidad decide si la publicación se muestra al instante o queda pendiente.
  @PreAuthorize("hasAnyRole('ESTUDIANTE','DOCENTE','ADMIN')")
  public Publicacion guardar(Long community, Long id, Requests.PublicacionDatos d) {
    Publicacion p =
        id == null ? new Publicacion() : repo.findById(id).orElseThrow(ApiException::missing);
    Long cid = id == null ? community : p.getIdComunidad();
    access.member(cid);
    if (id != null) access.owner(p.getIdAutor());
    String titulo = text.clean(d.titulo()), contenido = text.clean(d.contenido());
    if (titulo == null && contenido == null)
      throw ApiException.rule("Se requiere título o contenido");
    if (d.idSeccion() != null) {
      Seccion s = secciones.findById(d.idSeccion()).orElseThrow(ApiException::missing);
      if (!cursos.findById(s.getIdCurso()).orElseThrow().getIdComunidad().equals(cid))
        throw ApiException.rule("La sección no pertenece a la comunidad");
      access.teacher(d.idSeccion());
    }
    List<Long> files = d.archivos() == null ? List.of() : d.archivos().stream().distinct().toList();
    files.forEach(access::ownFile);
    boolean moderated = Boolean.TRUE.equals(access.comunidad(cid).getModeracionPrevia());
    PublicacionEstado previous = p.getEstado();
    if (id == null) {
      p.setIdComunidad(cid);
      p.setIdAutor(Actor.actual().id());
      p.setFechaPublicacion(LocalDateTime.now());
      p.setEliminado(false);
    } else p.setFechaEdicion(LocalDateTime.now());
    p.setTitulo(titulo);
    p.setContenido(contenido);
    p.setIdSeccion(d.idSeccion());
    p.setEstado(moderated ? PublicacionEstado.PENDIENTE : PublicacionEstado.APROBADA);
    p.setIdModerador(null);
    p.setFechaModeracion(null);
    p = repo.save(p);
    adjuntos.deleteAll(adjuntos.findByIdPublicacion(p.getIdPublicacion()));
    adjuntos.flush();
    long order = 1;
    for (Long file : files) {
      PublicacionArchivo a = new PublicacionArchivo();
      a.setIdPublicacion(p.getIdPublicacion());
      a.setIdArchivo(file);
      a.setOrden(order++);
      adjuntos.save(a);
    }
    if (p.getEstado() == PublicacionEstado.APROBADA && previous != PublicacionEstado.APROBADA)
      notifications.broadcast(
          cid,
          NotificacionTipo.NUEVA_PUBLICACION,
          "Nueva publicación educativa",
          "PUBLICACION",
          p.getIdPublicacion());
    audit.record(
        "PUBLICACION",
        p.getIdPublicacion(),
        id == null ? AuditoriaAccion.INSERT : AuditoriaAccion.UPDATE,
        null,
        "{\"estado\":\"" + p.getEstado() + "\"}");
    return p;
  }

  public void eliminar(Long id) {
    Publicacion p = repo.findById(id).orElseThrow(ApiException::missing);
    access.owner(p.getIdAutor());
    access.member(p.getIdComunidad());
    p.setEliminado(true);
    audit.record(
        "PUBLICACION", id, AuditoriaAccion.UPDATE, "{\"eliminado\":0}", "{\"eliminado\":1}");
  }

  public Publicacion moderar(Long id, Requests.Moderacion d) {
    Publicacion p = repo.findById(id).orElseThrow(ApiException::missing);
    access.moderator(p.getIdComunidad());
    if (d.estado() == PublicacionEstado.PENDIENTE)
      throw ApiException.rule("Apruebe o rechace la publicación");
    PublicacionEstado previous = p.getEstado();
    p.setEstado(d.estado());
    p.setIdModerador(Actor.actual().id());
    p.setFechaModeracion(LocalDateTime.now());
    if (previous != PublicacionEstado.APROBADA && p.getEstado() == PublicacionEstado.APROBADA)
      notifications.broadcast(
          p.getIdComunidad(),
          NotificacionTipo.NUEVA_PUBLICACION,
          "Nueva publicación educativa",
          "PUBLICACION",
          id);
    audit.record(
        "PUBLICACION",
        id,
        AuditoriaAccion.UPDATE,
        "{\"estado\":\"" + previous + "\"}",
        "{\"estado\":\"" + p.getEstado() + "\"}");
    return p;
  }

  public Comentario comentar(Long id, Requests.ComentarioDatos d) {
    Publicacion p = visible(id);
    if (p.getEstado() != PublicacionEstado.APROBADA)
      throw ApiException.rule("La publicación todavía no está aprobada");
    if (d.idComentarioPadre() != null) {
      Comentario parent =
          comentarios.findById(d.idComentarioPadre()).orElseThrow(ApiException::missing);
      if (!id.equals(parent.getIdPublicacion()))
        throw ApiException.rule("La respuesta debe pertenecer a la misma publicación");
    }
    Comentario c = new Comentario();
    c.setIdPublicacion(id);
    c.setIdComentarioPadre(d.idComentarioPadre());
    c.setIdAutor(Actor.actual().id());
    c.setContenido(text.required(d.contenido()));
    c.setFechaComentario(LocalDateTime.now());
    c.setEliminado(false);
    c = comentarios.save(c);
    notifications.comment(p.getIdAutor(), p.getIdComunidad(), c.getIdComentario());
    return c;
  }

  public void eliminarComentario(Long id) {
    Comentario c = comentarios.findById(id).orElseThrow(ApiException::missing);
    if (c.getIdPublicacion() != null) {
      Publicacion p = visible(c.getIdPublicacion());
      if (!c.getIdAutor().equals(Actor.actual().id())
          && !Actor.actual().admin()
          && !(Actor.actual().docente() && p.getIdAutor().equals(Actor.actual().id())))
        access.moderator(p.getIdComunidad());
    } else if (!c.getIdAutor().equals(Actor.actual().id())) throw ApiException.denied();
    c.setEliminado(true);
    c.setIdEliminadoPor(Actor.actual().id());
    audit.record(
        "COMENTARIO", id, AuditoriaAccion.UPDATE, "{\"eliminado\":0}", "{\"eliminado\":1}");
  }
}
