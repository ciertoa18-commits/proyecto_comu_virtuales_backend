package pe.edu.upeu.comunidades.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessService {
  private final ComunidadRepository comunidades;
  private final MiembroComunidadRepository miembros;
  private final SeccionRepository secciones;
  private final CursoRepository cursos;
  private final MatriculaRepository matriculas;
  private final ArchivoRepository archivos;

  public Comunidad comunidad(Long id) {
    Comunidad c = comunidades.findById(id).orElseThrow(ApiException::missing);
    if (c.getEstado() != ComunidadEstado.ACTIVA) throw ApiException.missing();
    return c;
  }

  public MiembroComunidad membership(Long id) {
    return miembros.findByIdComunidad(id).stream()
        .filter(
            m ->
                m.getIdUsuario().equals(Actor.actual().id())
                    && m.getEstado() == MiembroComunidadEstado.ACTIVO)
        .findFirst()
        .orElseThrow(ApiException::denied);
  }

  public void member(Long id) {
    comunidad(id);
    if (!Actor.actual().admin()) membership(id);
  }

  public void moderator(Long id) {
    member(id);
    if (!Actor.actual().admin()
        && membership(id).getRolComunidad() == MiembroComunidadRolComunidad.MIEMBRO)
      throw ApiException.denied();
  }

  public Seccion section(Long id) {
    Seccion s = secciones.findById(id).orElseThrow(ApiException::missing);
    Curso c = cursos.findById(s.getIdCurso()).orElseThrow(ApiException::missing);
    if (c.getEstado() != CursoEstado.ACTIVO) throw ApiException.missing();
    comunidad(c.getIdComunidad());
    return s;
  }

  public void teacher(Long id) {
    Seccion s = section(id);
    if (!Actor.actual().admin()
        && (!Actor.actual().docente() || !s.getIdDocente().equals(Actor.actual().id())))
      throw ApiException.denied();
    member(cursos.findById(s.getIdCurso()).orElseThrow().getIdComunidad());
  }

  @Transactional
  public void writableTeacher(Long id) {
    Seccion s = secciones.bloquear(id).orElseThrow(ApiException::missing);
    teacher(id);
    if (s.getEstado() == SeccionEstado.CERRADA)
      throw ApiException.conflict("La sección está cerrada");
  }

  public void enrolled(Long id) {
    Seccion s = section(id);
    if (Actor.actual().admin() || s.getIdDocente().equals(Actor.actual().id())) return;
    if (matriculas.findByIdSeccion(id).stream()
        .noneMatch(
            m ->
                m.getIdUsuario().equals(Actor.actual().id())
                    && m.getEstado() != MatriculaEstado.RETIRADO)) throw ApiException.denied();
    member(cursos.findById(s.getIdCurso()).orElseThrow().getIdComunidad());
  }

  public Archivo ownFile(Long id) {
    Archivo a = archivos.findById(id).orElseThrow(ApiException::missing);
    if (!a.getIdSubidoPor().equals(Actor.actual().id())) throw ApiException.denied();
    return a;
  }

  public void owner(Long id) {
    if (!Actor.actual().id().equals(id)) throw ApiException.denied();
  }
}
