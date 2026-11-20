package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.util.TextPolicy;

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
public class MensajeService {
  private final MensajeRepository repo;
  private final UsuarioRepository usuarios;
  private final TextPolicy text;
  private final NotificationService notifications;
  private final pe.perueduca.comunidades.realtime.TiempoRealService tiempoReal;

  public Mensaje send(Requests.MensajeDatos d) {
    if (Actor.actual().id().equals(d.idReceptor()))
      throw ApiException.rule("El destinatario debe ser otro usuario");
    Usuario u = usuarios.findById(d.idReceptor()).orElseThrow(ApiException::missing);
    if (u.getEstado() != UsuarioEstado.ACTIVO)
      throw ApiException.rule("El destinatario está inactivo");
    Mensaje m = new Mensaje();
    m.setIdEmisor(Actor.actual().id());
    m.setIdReceptor(d.idReceptor());
    m.setContenido(text.required(d.contenido()));
    m.setLeido(false);
    m.setEliminado(false);
    m.setFechaEnvio(java.time.LocalDateTime.now());
    m = repo.save(m);
    tiempoReal.avisar(d.idReceptor(), "MENSAJE");
    notifications.send(
        d.idReceptor(),
        null,
        NotificacionTipo.MENSAJE,
        "Nuevo mensaje",
        "MENSAJE",
        m.getIdMensaje());
    return m;
  }

  public Object list(Long con, int page, int size) {
    Long me = Actor.actual().id();
    return repo.findAll(
        (r, q, b) ->
            con == null
                ? b.or(b.equal(r.get("idEmisor"), me), b.equal(r.get("idReceptor"), me))
                : b.or(
                    b.and(b.equal(r.get("idEmisor"), me), b.equal(r.get("idReceptor"), con)),
                    b.and(b.equal(r.get("idEmisor"), con), b.equal(r.get("idReceptor"), me))),
        org.springframework.data.domain.PageRequest.of(
            Math.max(0, page),
            Math.max(1, Math.min(size, 100)),
            org.springframework.data.domain.Sort.by("fechaEnvio").descending()));
  }

  public void read(Long id) {
    Mensaje m = repo.findById(id).orElseThrow(ApiException::missing);
    if (!m.getIdReceptor().equals(Actor.actual().id())) throw ApiException.denied();
    m.setLeido(true);
  }
}
