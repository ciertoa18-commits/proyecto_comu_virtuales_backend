package pe.edu.upeu.comunidades.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.comunidades.dto.CuentaRequests;
import pe.edu.upeu.comunidades.dto.CuentaRequests.Destino;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

/**
 * Envío de avisos (CU12): a todos, a todos los docentes, a todos los estudiantes, a los alumnos de
 * una sección o a una persona. Se guarda una fila de NOTIFICACION por cada destinatario.
 *
 * <p>Permisos: ADMIN cualquier destino; DOCENTE solo a sus secciones o a una persona; ESTUDIANTE no
 * envía avisos.
 */
@Service
@Transactional
@lombok.RequiredArgsConstructor
public class AvisoService {
  private final UsuarioRepository usuarios;
  private final SeccionRepository secciones;
  private final MatriculaRepository matriculas;
  private final NotificacionRepository notificaciones;
  private final CuentaService cuentas;
  private final TextPolicy text;

  public record Envio(int enviadas, String mensaje) {}

  public Envio enviar(CuentaRequests.Aviso d) {
    Actor a = Actor.actual();
    if (a.estudiante()) throw ApiException.denied();
    if (a.docente() && d.destino() != Destino.SECCION && d.destino() != Destino.USUARIO)
      throw ApiException.denied();

    String titulo = text.required(d.titulo());
    String contenido = text.clean(d.contenido());
    Set<Long> destino = new LinkedHashSet<>();
    switch (d.destino()) {
      case TODOS -> activos(null).forEach(destino::add);
      case DOCENTES -> activos(RolCodigo.DOCENTE).forEach(destino::add);
      case ESTUDIANTES -> activos(RolCodigo.ESTUDIANTE).forEach(destino::add);
      case SECCION -> {
        if (d.idSeccion() == null) throw ApiException.rule("Elige una sección");
        Seccion s = secciones.findById(d.idSeccion()).orElseThrow(ApiException::missing);
        if (a.docente() && !a.id().equals(s.getIdDocente())) throw ApiException.denied();
        matriculas.findByIdSeccion(s.getIdSeccion()).stream()
            .filter(m -> m.getEstado() == MatriculaEstado.MATRICULADO)
            .map(Matricula::getIdUsuario)
            .forEach(destino::add);
      }
      case USUARIO -> {
        if (d.idUsuario() == null) throw ApiException.rule("Elige a la persona");
        Usuario u = usuarios.findById(d.idUsuario()).orElseThrow(ApiException::missing);
        if (u.getEstado() != UsuarioEstado.ACTIVO)
          throw ApiException.rule("La persona elegida no tiene una cuenta activa");
        destino.add(u.getIdUsuario());
      }
    }
    destino.remove(a.id());
    if (destino.isEmpty()) throw ApiException.rule("No hay destinatarios para ese envío");

    LocalDateTime ahora = LocalDateTime.now();
    List<Notificacion> filas = new ArrayList<>();
    for (Long id : destino) {
      Notificacion n = new Notificacion();
      n.setIdDestinatario(id);
      n.setIdEmisor(a.id());
      n.setTipo(NotificacionTipo.AVISO);
      n.setTitulo(titulo);
      n.setContenido(contenido);
      n.setLeida(false);
      n.setFechaCreacion(ahora);
      filas.add(n);
    }
    notificaciones.saveAll(filas);
    return new Envio(
        filas.size(),
        "Aviso enviado a " + filas.size() + (filas.size() == 1 ? " persona" : " personas"));
  }

  private List<Long> activos(RolCodigo rol) {
    Long idRol = rol == null ? null : cuentas.idRol(rol);
    return (idRol == null ? usuarios.findAll() : usuarios.findByIdRol(idRol)).stream()
        .filter(u -> u.getEstado() == UsuarioEstado.ACTIVO)
        .map(Usuario::getIdUsuario)
        .toList();
  }
}
