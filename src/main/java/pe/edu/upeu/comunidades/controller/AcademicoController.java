package pe.edu.upeu.comunidades.controller;

import java.util.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;
import pe.edu.upeu.comunidades.service.impl.*;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class AcademicoController {
  private final AcademicoService service;
  private final AccessService access;
  private final CursoRepository cursos;
  private final SeccionRepository secciones;
  private final UnidadRepository unidades;
  private final SesionRepository sesiones;
  private final RecursoRepository recursos;
  private final MatriculaRepository matriculas;
  private final NotaRepository notas;
  private final EvaluacionRepository evaluaciones;
  private final AsistenciaRepository asistencias;
  private final EntregaTareaRepository entregas;
  private final EventoRepository eventos;
  private final UsuarioRepository usuarios;

  /** Nombre del estudiante para que el docente no vea solo un número. */
  private Map<String, Object> alumno(Long idUsuario) {
    return usuarios
        .findById(idUsuario)
        .<Map<String, Object>>map(
            u -> Map.of("idUsuario", u.getIdUsuario(), "nombres", u.getNombres(), "apellidos", u.getApellidos()))
        .orElse(Map.of("idUsuario", idUsuario, "nombres", "", "apellidos", ""));
  }

  @GetMapping("/comunidades/{id}/cursos")
  public Object courses(@PathVariable Long id) {
    access.member(id);
    return cursos.findByIdComunidad(id).stream()
        .filter(c -> c.getEstado() == CursoEstado.ACTIVO)
        .toList();
  }

  @PostMapping("/comunidades/{id}/cursos")
  public Object createCourse(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.CursoDatos d) {
    return service.guardarCurso(id, null, d);
  }

  @PutMapping("/cursos/{id}")
  public Object editCourse(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.CursoDatos d) {
    return service.guardarCurso(null, id, d);
  }

  @DeleteMapping("/cursos/{id}")
  public void deleteCourse(@PathVariable Long id) {
    service.eliminarCurso(id);
  }

  @GetMapping("/cursos/{id}")
  public Object course(@PathVariable Long id) {
    Curso c = service.curso(id);
    access.member(c.getIdComunidad());
    return c;
  }

  @GetMapping("/cursos/{id}/secciones")
  public Object sections(@PathVariable Long id) {
    service.curso(id);
    return secciones.findByIdCurso(id);
  }

  @PostMapping("/cursos/{id}/secciones")
  public Object createSection(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.SeccionDatos d) {
    return service.crearSeccion(id, d);
  }

  @GetMapping("/secciones/{id}")
  public Object section(@PathVariable Long id) {
    access.enrolled(id);
    return access.section(id);
  }

  @GetMapping("/secciones/{id}/unidades")
  public Object units(@PathVariable Long id) {
    access.enrolled(id);
    return unidades.findByIdSeccion(id).stream()
        .sorted(Comparator.comparing(Unidad::getOrden))
        .toList();
  }

  @PostMapping("/secciones/{id}/unidades")
  public Object createUnit(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.UnidadDatos d) {
    return service.guardarUnidad(id, null, d);
  }

  @PutMapping("/unidades/{id}")
  public Object editUnit(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.UnidadDatos d) {
    return service.guardarUnidad(null, id, d);
  }

  @DeleteMapping("/unidades/{id}")
  public void deleteUnit(@PathVariable Long id) {
    service.eliminarUnidad(id);
  }

  @GetMapping("/unidades/{id}/sesiones")
  public Object sessions(@PathVariable Long id) {
    service.unidad(id);
    return sesiones.findByIdUnidad(id).stream()
        .sorted(Comparator.comparing(Sesion::getOrden))
        .toList();
  }

  @PostMapping("/unidades/{id}/sesiones")
  public Object createSession(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.SesionDatos d) {
    return service.guardarSesion(id, null, d);
  }

  @PutMapping("/sesiones/{id}")
  public Object editSession(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.SesionDatos d) {
    return service.guardarSesion(null, id, d);
  }

  @GetMapping("/me/clases")
  public Object classes() {
    return matriculas.findByIdUsuario(Actor.actual().id()).stream()
        .filter(m -> m.getEstado() != MatriculaEstado.RETIRADO)
        .flatMap(
            m -> {
              access.enrolled(m.getIdSeccion());
              Seccion s = access.section(m.getIdSeccion());
              return unidades.findByIdSeccion(s.getIdSeccion()).stream()
                  .flatMap(u -> sesiones.findByIdUnidad(u.getIdUnidad()).stream())
                  .map(
                      x -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("sesion", x);
                        item.put(
                            "enlace",
                            x.getEnlaceClase() == null ? s.getSalaVirtual() : x.getEnlaceClase());
                        return item;
                      });
            })
        .toList();
  }

  @GetMapping("/unidades/{id}/recursos")
  public Object resources(@PathVariable Long id) {
    service.unidad(id);
    return recursos.findByIdUnidad(id);
  }

  @PostMapping("/unidades/{id}/recursos")
  public Object createResource(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.RecursoDatos d) {
    return service.guardarRecurso(id, null, d);
  }

  @PutMapping("/recursos/{id}")
  public Object editResource(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.RecursoDatos d) {
    return service.guardarRecurso(null, id, d);
  }

  @DeleteMapping("/recursos/{id}")
  public void deleteResource(@PathVariable Long id) {
    service.eliminarRecurso(id);
  }

  @GetMapping("/recursos/{id}/descarga")
  public Object download(@PathVariable Long id) {
    Recurso r = service.recurso(id);
    if (r.getIdArchivo() == null)
      throw pe.edu.upeu.comunidades.exception.ApiException.rule("El recurso no tiene archivo");
    return Map.of("url", "/api/v1/archivos/" + r.getIdArchivo() + "/descarga");
  }

  @GetMapping("/me/recursos")
  public Object myResources() {
    return matriculas.findByIdUsuario(Actor.actual().id()).stream()
        .filter(m -> m.getEstado() != MatriculaEstado.RETIRADO)
        .flatMap(
            m -> {
              access.enrolled(m.getIdSeccion());
              return unidades.findByIdSeccion(m.getIdSeccion()).stream()
                  .flatMap(u -> recursos.findByIdUnidad(u.getIdUnidad()).stream());
            })
        .toList();
  }

  @GetMapping("/comunidades/{id}/eventos")
  public Object events(@PathVariable Long id) {
    access.member(id);
    return eventos.findByIdComunidad(id);
  }

  @PostMapping("/comunidades/{id}/eventos")
  public Object createEvent(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.EventoDatos d) {
    return service.evento(id, d);
  }

  @PostMapping("/secciones/{id}/matriculas")
  public Object enroll(@PathVariable Long id, @RequestParam(required = false) Long idUsuario) {
    return service.matricular(id, idUsuario);
  }

  @PutMapping("/matriculas/{id}/retiro")
  public void withdraw(@PathVariable Long id) {
    service.retirar(id);
  }

  @GetMapping("/secciones/{id}/evaluaciones")
  public Object evaluations(@PathVariable Long id) {
    access.teacher(id);
    return evaluaciones.findByIdSeccion(id);
  }

  @PostMapping("/secciones/{id}/evaluaciones")
  public Object evaluation(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.EvaluacionDatos d) {
    return service.evaluar(id, d);
  }

  @PutMapping("/notas")
  public Object grade(@jakarta.validation.Valid @RequestBody Requests.NotaDatos d) {
    return service.nota(d);
  }

  @GetMapping("/me/notas")
  public Object myGrades() {
    return matriculas.findByIdUsuario(Actor.actual().id()).stream()
        .map(
            m -> {
              Seccion sec = secciones.findById(m.getIdSeccion()).orElse(null);
              String curso =
                  sec == null ? "" : cursos.findById(sec.getIdCurso()).map(Curso::getNombre).orElse("");
              return Map.of(
                  "matricula",
                  m,
                  "curso",
                  curso,
                  "grupo",
                  sec == null ? "" : sec.getGrupo(),
                  "evaluaciones",
                  evaluaciones.findByIdSeccion(m.getIdSeccion()),
                  "notas",
                  notas.findByIdMatricula(m.getIdMatricula()),
                  "promedio",
                  service.promedio(m.getIdMatricula()));
            })
        .toList();
  }

  @GetMapping("/secciones/{id}/notas")
  public Object grades(@PathVariable Long id) {
    access.teacher(id);
    return matriculas.findByIdSeccion(id).stream()
        .map(
            m ->
                Map.of(
                    "matricula",
                    m,
                    "estudiante",
                    alumno(m.getIdUsuario()),
                    "notas",
                    notas.findByIdMatricula(m.getIdMatricula()),
                    "promedio",
                    service.promedio(m.getIdMatricula())))
        .toList();
  }

  @PostMapping("/secciones/{id}/cerrar")
  public void close(@PathVariable Long id) {
    service.cerrar(id);
  }

  @GetMapping("/secciones/{id}/ranking")
  public Object ranking(@PathVariable Long id) {
    access.teacher(id);
    return matriculas.findByIdSeccion(id).stream()
        .filter(m -> m.getEstado() != MatriculaEstado.RETIRADO)
        .sorted(
            Comparator.comparing((Matricula m) -> service.promedio(m.getIdMatricula())).reversed())
        .map(
            m ->
                Map.of(
                    "idUsuario",
                    m.getIdUsuario(),
                    "estudiante",
                    alumno(m.getIdUsuario()),
                    "promedio",
                    service.promedio(m.getIdMatricula())))
        .toList();
  }

  @GetMapping("/secciones/{id}/riesgo")
  public Object risk(@PathVariable Long id) {
    access.teacher(id);
    return matriculas.findByIdSeccion(id).stream()
        .filter(
            m ->
                m.getEstado() == MatriculaEstado.MATRICULADO
                    && service
                            .promedio(m.getIdMatricula())
                            .compareTo(new java.math.BigDecimal("10.5"))
                        < 0)
        .map(
            m ->
                Map.of(
                    "matricula",
                    m,
                    "estudiante",
                    alumno(m.getIdUsuario()),
                    "promedio",
                    service.promedio(m.getIdMatricula())))
        .toList();
  }

  @GetMapping("/sesiones/{id}/asistencia")
  public Object attendance(@PathVariable Long id) {
    var s =
        sesiones.findById(id).orElseThrow(pe.edu.upeu.comunidades.exception.ApiException::missing);
    access.teacher(unidades.findById(s.getIdUnidad()).orElseThrow().getIdSeccion());
    return asistencias.findByIdSesion(id);
  }

  @PostMapping("/sesiones/{id}/asistencia")
  public Object recordAttendance(
      @PathVariable Long id,
      @jakarta.validation.Valid @RequestBody
          List<Requests.@jakarta.validation.Valid AsistenciaDatos> datos) {
    return service.asistencia(id, datos);
  }

  @PostMapping("/tareas/{id}/entregas")
  public Object submit(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.EntregaDatos d) {
    return service.entregar(id, d);
  }

  @GetMapping("/tareas/{id}/entregas")
  public Object submissions(@PathVariable Long id) {
    var r = service.recurso(id);
    access.teacher(unidades.findById(r.getIdUnidad()).orElseThrow().getIdSeccion());
    return entregas.findByIdRecurso(id);
  }
}
