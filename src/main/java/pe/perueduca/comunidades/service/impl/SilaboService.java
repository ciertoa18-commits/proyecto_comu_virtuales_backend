package pe.perueduca.comunidades.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.enums.AsistenciaEstado;
import pe.perueduca.comunidades.enums.MatriculaEstado;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;
import pe.perueduca.comunidades.util.TextPolicy;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class SilaboService {
  private final SilaboRepository silabos;
  private final SeccionRepository secciones;
  private final CursoRepository cursos;
  private final MatriculaRepository matriculas;
  private final AsistenciaRepository asistencias;
  private final EvaluacionRepository evaluaciones;
  private final NotaRepository notas;
  private final UnidadRepository unidades;
  private final RecursoRepository recursos;
  private final UsuarioRepository usuarios;
  private final AccessService access;
  private final AcademicoService academico;
  private final TextPolicy text;

  public record ResumenAsistencia(
      int total, int puntuales, int tardanzas, int faltas, int justificadas, int porcentaje) {}

  public record ResumenCurso(
      Long idSeccion,
      Long idMatricula,
      String curso,
      String codigo,
      String grupo,
      String docente,
      String estadoSeccion,
      String estadoMatricula,
      BigDecimal promedio,
      int evaluaciones,
      int notas,
      ResumenAsistencia asistencia,
      int recursos,
      int alumnos,
      boolean tieneSilabo) {}

  @Transactional(readOnly = true)
  public Optional<Silabo> ver(Long idSeccion) {
    access.enrolled(idSeccion);
    return silabos.findByIdSeccion(idSeccion);
  }

  public Silabo guardar(Long idSeccion, Requests.SilaboDatos d) {
    access.teacher(idSeccion);
    if (d.idArchivo() != null) {
      Archivo f = access.ownFile(d.idArchivo());
      if (!"pdf".equalsIgnoreCase(f.getExtension()))
        throw ApiException.rule("El archivo del sílabo debe ser PDF");
    }
    Silabo s = silabos.findByIdSeccion(idSeccion).orElseGet(Silabo::new);
    s.setIdSeccion(idSeccion);
    s.setSumilla(text.clean(d.sumilla()));
    s.setCompetencias(text.clean(d.competencias()));
    s.setContenidos(text.clean(d.contenidos()));
    s.setMetodologia(text.clean(d.metodologia()));
    s.setEvaluacion(text.clean(d.evaluacion()));
    s.setBibliografia(text.clean(d.bibliografia()));
    s.setIdArchivo(d.idArchivo());
    s.setIdAutor(Actor.actual().id());
    s.setFechaActualizacion(LocalDateTime.now());
    if (s.getSumilla() == null && s.getContenidos() == null && s.getIdArchivo() == null)
      throw ApiException.rule(
          "Escribe al menos la sumilla o los contenidos, o adjunta el sílabo en PDF");
    return silabos.save(s);
  }

  @Transactional(readOnly = true)
  public List<ResumenCurso> misCursos() {
    Actor a = Actor.actual();
    if (a.estudiante())
      return matriculas.findByIdUsuario(a.id()).stream()
          .filter(m -> m.getEstado() != MatriculaEstado.RETIRADO)
          .map(this::deAlumno)
          .filter(Objects::nonNull)
          .toList();
    List<Seccion> lista = a.admin() ? secciones.findAll() : secciones.findByIdDocente(a.id());
    return lista.stream().map(s -> resumen(s, null)).toList();
  }

  private ResumenCurso deAlumno(Matricula m) {
    return secciones.findById(m.getIdSeccion()).map(s -> resumen(s, m)).orElse(null);
  }

  private ResumenCurso resumen(Seccion s, Matricula m) {
    Curso c = cursos.findById(s.getIdCurso()).orElse(null);
    String docente =
        usuarios
            .findById(s.getIdDocente())
            .map(u -> u.getNombres() + " " + u.getApellidos())
            .orElse("");
    int totalRecursos =
        unidades.findByIdSeccion(s.getIdSeccion()).stream()
            .mapToInt(u -> recursos.findByIdUnidad(u.getIdUnidad()).size())
            .sum();
    int alumnos =
        (int)
            matriculas.findByIdSeccion(s.getIdSeccion()).stream()
                .filter(x -> x.getEstado() != MatriculaEstado.RETIRADO)
                .count();
    return new ResumenCurso(
        s.getIdSeccion(),
        m == null ? null : m.getIdMatricula(),
        c == null ? "" : c.getNombre(),
        c == null ? "" : c.getCodigo(),
        s.getGrupo(),
        docente,
        s.getEstado() == null ? null : s.getEstado().name(),
        m == null ? null : m.getEstado().name(),
        m == null ? null : academico.promedio(m.getIdMatricula()),
        evaluaciones.findByIdSeccion(s.getIdSeccion()).size(),
        m == null ? 0 : notas.findByIdMatricula(m.getIdMatricula()).size(),
        m == null ? null : asistencia(m.getIdMatricula()),
        totalRecursos,
        alumnos,
        silabos.existsByIdSeccion(s.getIdSeccion()));
  }

  private ResumenAsistencia asistencia(Long idMatricula) {
    List<Asistencia> lista = asistencias.findByIdMatricula(idMatricula);
    int puntuales = contar(lista, AsistenciaEstado.PRESENTE);
    int tardanzas = contar(lista, AsistenciaEstado.TARDANZA);
    int faltas = contar(lista, AsistenciaEstado.FALTA);
    int justificadas = contar(lista, AsistenciaEstado.JUSTIFICADA);
    int total = lista.size();
    int porcentaje =
        total == 0 ? 100 : Math.round((puntuales + tardanzas + justificadas) * 100f / total);
    return new ResumenAsistencia(total, puntuales, tardanzas, faltas, justificadas, porcentaje);
  }

  private static int contar(List<Asistencia> lista, AsistenciaEstado e) {
    return (int) lista.stream().filter(a -> a.getEstado() == e).count();
  }
}
