package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.AuditoriaAccion;
import pe.perueduca.comunidades.enums.CursoEstado;
import pe.perueduca.comunidades.enums.EntregaTareaEstado;
import pe.perueduca.comunidades.enums.MatriculaEstado;
import pe.perueduca.comunidades.enums.MiembroComunidadEstado;
import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.RecursoTipo;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.SeccionEstado;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.util.TextPolicy;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class AcademicoService {
  private final CursoRepository cursos;
  private final SeccionRepository secciones;
  private final UnidadRepository unidades;
  private final SesionRepository sesiones;
  private final RecursoRepository recursos;
  private final TareaRepository tareas;
  private final MatriculaRepository matriculas;
  private final EvaluacionRepository evaluaciones;
  private final NotaRepository notas;
  private final AsistenciaRepository asistencias;
  private final EntregaTareaRepository entregas;
  private final EventoRepository eventos;
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PeriodoAcademicoRepository periodos;
  private final AccessService access;
  private final TextPolicy text;
  private final NotificationService notifications;
  private final AuditService audit;
  private final PermisoNotaService permisosNota;

  public Curso curso(Long id) {
    Curso c = cursos.findById(id).orElseThrow(ApiException::missing);
    if (c.getEstado() != CursoEstado.ACTIVO) throw ApiException.missing();
    access.member(c.getIdComunidad());
    return c;
  }

  public Unidad unidad(Long id) {
    Unidad u = unidades.findById(id).orElseThrow(ApiException::missing);
    access.enrolled(u.getIdSeccion());
    return u;
  }

  public Recurso recurso(Long id) {
    Recurso r = recursos.findById(id).orElseThrow(ApiException::missing);
    unidad(r.getIdUnidad());
    return r;
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Curso guardarCurso(Long community, Long id, Requests.CursoDatos d) {
    Curso c = id == null ? new Curso() : curso(id);
    access.comunidad(id == null ? community : c.getIdComunidad());
    c.setCodigo(text.required(d.codigo()));
    c.setNombre(text.required(d.nombre()));
    c.setDescripcion(text.clean(d.descripcion()));
    if (id == null) {
      c.setIdComunidad(community);
      c.setEstado(CursoEstado.ACTIVO);
      c.setFechaCreacion(LocalDateTime.now());
    }
    return cursos.save(c);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void eliminarCurso(Long id) {
    curso(id).setEstado(CursoEstado.INACTIVO);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Seccion crearSeccion(Long id, Requests.SeccionDatos d) {
    curso(id);
    Usuario u = usuarios.findById(d.idDocente()).orElseThrow(ApiException::missing);
    if (u.getEstado() != UsuarioEstado.ACTIVO
        || roles.findById(u.getIdRol()).orElseThrow().getCodigo() != RolCodigo.DOCENTE)
      throw ApiException.rule("El titular debe ser un docente activo");
    if (!periodos.existsById(d.idPeriodo())) throw ApiException.missing();
    text.url(d.salaVirtual());
    Seccion s = new Seccion();
    s.setIdCurso(id);
    s.setIdPeriodo(d.idPeriodo());
    s.setIdDocente(d.idDocente());
    s.setGrupo(text.required(d.grupo()));
    s.setCupoMaximo(d.cupoMaximo());
    s.setSalaVirtual(d.salaVirtual());
    s.setEstado(SeccionEstado.ABIERTA);
    return secciones.save(s);
  }

  public Unidad guardarUnidad(Long section, Long id, Requests.UnidadDatos d) {
    Unidad u = id == null ? new Unidad() : unidades.findById(id).orElseThrow(ApiException::missing);
    Long sid = id == null ? section : u.getIdSeccion();
    access.writableTeacher(sid);
    u.setIdSeccion(sid);
    u.setNombre(text.required(d.nombre()));
    u.setDescripcion(text.clean(d.descripcion()));
    u.setOrden(d.orden());
    return unidades.save(u);
  }

  public void eliminarUnidad(Long id) {
    Unidad u = unidades.findById(id).orElseThrow(ApiException::missing);
    access.writableTeacher(u.getIdSeccion());
    if (!sesiones.findByIdUnidad(id).isEmpty()
        || !recursos.findByIdUnidad(id).isEmpty()
        || !evaluaciones.findByIdUnidad(id).isEmpty())
      throw ApiException.conflict("La unidad contiene sesiones, recursos o evaluaciones");
    unidades.delete(u);
  }

  public Sesion guardarSesion(Long unit, Long id, Requests.SesionDatos d) {
    Sesion s = id == null ? new Sesion() : sesiones.findById(id).orElseThrow(ApiException::missing);
    Long uid = id == null ? unit : s.getIdUnidad();
    Unidad u = unidades.findById(uid).orElseThrow(ApiException::missing);
    access.writableTeacher(u.getIdSeccion());
    text.url(d.enlaceClase());
    s.setIdUnidad(uid);
    s.setTitulo(text.required(d.titulo()));
    s.setOrden(d.orden());
    s.setFechaInicio(d.fechaInicio());
    s.setDuracionMin(d.duracionMin());
    s.setEnlaceClase(d.enlaceClase());
    return sesiones.save(s);
  }

  public Recurso guardarRecurso(Long unit, Long id, Requests.RecursoDatos d) {
    Recurso r =
        id == null ? new Recurso() : recursos.findById(id).orElseThrow(ApiException::missing);
    Long uid = id == null ? unit : r.getIdUnidad();
    Unidad u = unidades.findById(uid).orElseThrow(ApiException::missing);
    access.writableTeacher(u.getIdSeccion());
    if (id != null) {
      access.owner(r.getIdAutor());
      if (r.getTipo() != d.tipo())
        throw ApiException.rule("No se puede cambiar el tipo de un recurso existente");
    }
    if (d.idSesion() != null
        && !sesiones
            .findById(d.idSesion())
            .orElseThrow(ApiException::missing)
            .getIdUnidad()
            .equals(uid)) throw ApiException.rule("La sesión pertenece a otra unidad");
    if (d.tipo() == RecursoTipo.DOCUMENTO || d.tipo() == RecursoTipo.VIDEO) {
      if (d.idArchivo() == null) throw ApiException.rule("Adjunte un archivo");
      Archivo a = access.ownFile(d.idArchivo());
      if (d.tipo() == RecursoTipo.VIDEO && !a.getExtension().equals("mp4")
          || d.tipo() == RecursoTipo.DOCUMENTO && !Set.of("pdf", "docx").contains(a.getExtension()))
        throw ApiException.rule("El archivo no corresponde al tipo de recurso");
    }
    if (d.tipo() == RecursoTipo.ENLACE) {
      if (d.urlExterna() == null) throw ApiException.rule("Indique el enlace");
      text.url(d.urlExterna());
    }
    if (d.tipo() == RecursoTipo.TAREA && d.fechaEntrega() == null)
      throw ApiException.rule("Indique la fecha límite de la tarea");
    if (d.idEvaluacion() != null
        && !evaluaciones
            .findById(d.idEvaluacion())
            .orElseThrow(ApiException::missing)
            .getIdSeccion()
            .equals(u.getIdSeccion()))
      throw ApiException.rule("La evaluación pertenece a otra sección");
    r.setIdUnidad(uid);
    r.setIdSesion(d.idSesion());
    r.setTitulo(text.required(d.titulo()));
    r.setDescripcion(text.clean(d.descripcion()));
    r.setTipo(d.tipo());
    r.setIdIdioma(d.idIdioma());
    r.setIdArchivo(d.idArchivo());
    r.setUrlExterna(d.urlExterna());
    if (id == null) {
      r.setIdAutor(Actor.actual().id());
      r.setFechaPublicacion(LocalDateTime.now());
      r.setEliminado(false);
    }
    r = recursos.save(r);
    if (d.tipo() == RecursoTipo.TAREA) {
      Tarea t = id == null ? new Tarea() : tareas.findById(id).orElseThrow(ApiException::missing);
      t.setIdRecurso(r.getIdRecurso());
      t.setFechaEntrega(d.fechaEntrega());
      t.setPermiteTardias(d.permiteTardias());
      t.setIdEvaluacion(d.idEvaluacion());
      tareas.save(t);
    }
    if (id == null)
      notifications.broadcast(
          cursos
              .findById(access.section(u.getIdSeccion()).getIdCurso())
              .orElseThrow()
              .getIdComunidad(),
          NotificacionTipo.NUEVO_RECURSO,
          "Nuevo recurso educativo",
          "RECURSO",
          r.getIdRecurso());
    return r;
  }

  public void eliminarRecurso(Long id) {
    Recurso r = recurso(id);
    access.writableTeacher(unidades.findById(r.getIdUnidad()).orElseThrow().getIdSeccion());
    access.owner(r.getIdAutor());
    r.setEliminado(true);
    r.setFechaEliminacion(LocalDateTime.now());
  }

  @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
  public Evento evento(Long community, Requests.EventoDatos d) {
    access.member(community);
    if (d.fechaFin() != null && d.fechaFin().isBefore(d.fechaInicio()))
      throw ApiException.rule("La fecha final es anterior a la inicial");
    if (d.idSeccion() != null
        && !cursos
            .findById(access.section(d.idSeccion()).getIdCurso())
            .orElseThrow()
            .getIdComunidad()
            .equals(community)) throw ApiException.rule("La sección pertenece a otra comunidad");
    text.url(d.enlace());
    Evento e = new Evento();
    e.setIdComunidad(community);
    e.setIdSeccion(d.idSeccion());
    e.setIdCreador(Actor.actual().id());
    e.setTitulo(text.required(d.titulo()));
    e.setDescripcion(text.clean(d.descripcion()));
    e.setFechaInicio(d.fechaInicio());
    e.setFechaFin(d.fechaFin());
    e.setEnlace(d.enlace());
    e = eventos.save(e);
    notifications.broadcast(
        community, NotificacionTipo.EVENTO, "Nuevo evento académico", "EVENTO", e.getIdEvento());
    return e;
  }

  @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
  public Matricula matricular(Long id, Long usuario) {
    Long uid = usuario == null ? Actor.actual().id() : usuario;
    if (!Actor.actual().admin() && !Actor.actual().id().equals(uid)) throw ApiException.denied();
    Seccion s = secciones.bloquear(id).orElseThrow(ApiException::missing);
    access.section(id);
    if (s.getEstado() == SeccionEstado.CERRADA)
      throw ApiException.conflict("La sección está cerrada");
    Usuario u = usuarios.findById(uid).orElseThrow(ApiException::missing);
    if (u.getEstado() != UsuarioEstado.ACTIVO
        || roles.findById(u.getIdRol()).orElseThrow().getCodigo() != RolCodigo.ESTUDIANTE)
      throw ApiException.rule("Solo se matriculan estudiantes activos");
    Long community = cursos.findById(s.getIdCurso()).orElseThrow().getIdComunidad();
    if (miembros.findByIdComunidad(community).stream()
        .noneMatch(
            m -> m.getIdUsuario().equals(uid) && m.getEstado() == MiembroComunidadEstado.ACTIVO))
      throw ApiException.rule("El estudiante debe pertenecer a la comunidad");
    List<Matricula> all = matriculas.findByIdSeccion(id);
    if (all.stream().anyMatch(m -> m.getIdUsuario().equals(uid)))
      throw ApiException.conflict("Ya existe una matrícula para el estudiante");
    if (s.getCupoMaximo() != null
        && all.stream().filter(m -> m.getEstado() == MatriculaEstado.MATRICULADO).count()
            >= s.getCupoMaximo()) throw ApiException.conflict("No quedan cupos disponibles");
    Matricula m = new Matricula();
    m.setIdSeccion(id);
    m.setIdUsuario(uid);
    m.setEstado(MatriculaEstado.MATRICULADO);
    m.setFechaMatricula(LocalDateTime.now());
    return matriculas.save(m);
  }

  private final MiembroComunidadRepository miembros;

  public void retirar(Long id) {
    Matricula m = matriculas.findById(id).orElseThrow(ApiException::missing);
    if (!Actor.actual().admin()) access.owner(m.getIdUsuario());
    if (access.section(m.getIdSeccion()).getEstado() == SeccionEstado.CERRADA)
      throw ApiException.conflict("La sección está cerrada");
    m.setEstado(MatriculaEstado.RETIRADO);
    m.setFechaRetiro(LocalDateTime.now());
  }

  public Evaluacion evaluar(Long section, Requests.EvaluacionDatos d) {
    access.writableTeacher(section);
    if (d.idUnidad() != null
        && !unidades
            .findById(d.idUnidad())
            .orElseThrow(ApiException::missing)
            .getIdSeccion()
            .equals(section)) throw ApiException.rule("La unidad pertenece a otra sección");
    java.math.BigDecimal suma =
        evaluaciones.findByIdSeccion(section).stream()
            .map(Evaluacion::getPeso)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    if (suma.add(d.peso()).compareTo(new java.math.BigDecimal("100")) > 0)
      throw ApiException.rule(
          "Los pesos de las evaluaciones no pueden sumar más de 100% (ya hay "
              + suma.stripTrailingZeros().toPlainString()
              + "%)");
    Evaluacion e = new Evaluacion();
    e.setIdSeccion(section);
    e.setIdUnidad(d.idUnidad());
    e.setNombre(text.required(d.nombre()));
    e.setTipo(d.tipo());
    e.setPeso(d.peso());
    e.setFecha(d.fecha());
    return evaluaciones.save(e);
  }

  public Nota nota(Requests.NotaDatos d) {
    Matricula m = matriculas.findById(d.idMatricula()).orElseThrow(ApiException::missing);
    access.writableTeacher(m.getIdSeccion());
    if (m.getEstado() != MatriculaEstado.MATRICULADO)
      throw ApiException.rule("La matrícula no está activa");
    Evaluacion e = evaluaciones.findById(d.idEvaluacion()).orElseThrow(ApiException::missing);
    if (!e.getIdSeccion().equals(m.getIdSeccion()))
      throw ApiException.rule("La evaluación y la matrícula deben pertenecer a la misma sección");
    Nota n =
        notas.findByIdMatricula(m.getIdMatricula()).stream()
            .filter(x -> x.getIdEvaluacion().equals(e.getIdEvaluacion()))
            .findFirst()
            .orElseGet(Nota::new);
    String before =
        n.getCalificacion() == null ? null : "{\"calificacion\":" + n.getCalificacion() + "}";
    boolean nuevo = n.getIdNota() == null;
    if (!nuevo
        && !Actor.actual().admin()
        && n.getCalificacion() != null
        && n.getCalificacion().compareTo(d.calificacion()) != 0
        && !permisosNota.tienePermiso(m.getIdSeccion(), Actor.actual().id()))
      throw ApiException.rule("Necesitas permiso del administrador para modificar una nota ya registrada");
    n.setIdMatricula(m.getIdMatricula());
    n.setIdEvaluacion(e.getIdEvaluacion());
    n.setCalificacion(d.calificacion());
    n.setComentarioDocente(text.clean(d.comentarioDocente()));
    n.setIdRegistradoPor(Actor.actual().id());
    if (nuevo) n.setFechaRegistro(LocalDateTime.now());
    else n.setFechaModificacion(LocalDateTime.now());
    n = notas.save(n);
    audit.record(
        "NOTA",
        n.getIdNota(),
        nuevo ? AuditoriaAccion.INSERT : AuditoriaAccion.UPDATE,
        before,
        "{\"calificacion\":" + n.getCalificacion() + "}");
    return n;
  }

  public BigDecimal promedio(Long id) {
    Matricula m = matriculas.findById(id).orElseThrow(ApiException::missing);
    BigDecimal result = BigDecimal.ZERO;
    Map<Long, BigDecimal> pesos = new HashMap<>();
    evaluaciones
        .findByIdSeccion(m.getIdSeccion())
        .forEach(e -> pesos.put(e.getIdEvaluacion(), e.getPeso()));
    for (Nota n : notas.findByIdMatricula(id))
      result =
          result.add(
              n.getCalificacion()
                  .multiply(pesos.getOrDefault(n.getIdEvaluacion(), BigDecimal.ZERO))
                  .divide(BigDecimal.valueOf(100)));
    return result.setScale(2, RoundingMode.HALF_UP);
  }

  public void cerrar(Long id) {
    Seccion s = secciones.bloquear(id).orElseThrow(ApiException::missing);
    access.writableTeacher(id);
    BigDecimal total =
        evaluaciones.findByIdSeccion(id).stream()
            .map(Evaluacion::getPeso)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(BigDecimal.valueOf(100)) != 0)
      throw ApiException.rule("Los pesos deben sumar 100 %");
    for (Matricula m : matriculas.findByIdSeccion(id))
      if (m.getEstado() == MatriculaEstado.MATRICULADO) {
        BigDecimal p = promedio(m.getIdMatricula());
        m.setNotaFinal(p);
        m.setEstado(
            p.compareTo(new BigDecimal("10.5")) >= 0
                ? MatriculaEstado.APROBADO
                : MatriculaEstado.DESAPROBADO);
      }
    s.setEstado(SeccionEstado.CERRADA);
  }

  public Object asistencia(Long session, List<Requests.AsistenciaDatos> datos) {
    Sesion s = sesiones.findById(session).orElseThrow(ApiException::missing);
    Long section = unidades.findById(s.getIdUnidad()).orElseThrow().getIdSeccion();
    access.writableTeacher(section);
    Set<Long> seen = new HashSet<>();
    List<Asistencia> result = new ArrayList<>();
    for (var d : datos) {
      if (!seen.add(d.idMatricula()))
        throw ApiException.rule("Matrícula duplicada en el registro de asistencia");
      if (d.estado() == pe.perueduca.comunidades.enums.AsistenciaEstado.JUSTIFICADA
          && text.clean(d.observacion()) == null)
        throw ApiException.rule("Indica la razón de cada falta justificada");
      Matricula m = matriculas.findById(d.idMatricula()).orElseThrow(ApiException::missing);
      if (!m.getIdSeccion().equals(section) || m.getEstado() != MatriculaEstado.MATRICULADO)
        throw ApiException.rule("Matrícula inválida para esta sesión");
      Asistencia a =
          asistencias.findByIdSesion(session).stream()
              .filter(x -> x.getIdMatricula().equals(d.idMatricula()))
              .findFirst()
              .orElseGet(Asistencia::new);
      a.setIdSesion(session);
      a.setIdMatricula(m.getIdMatricula());
      a.setEstado(d.estado());
      a.setObservacion(text.clean(d.observacion()));
      a.setIdRegistradoPor(Actor.actual().id());
      a.setFechaRegistro(LocalDateTime.now());
      result.add(asistencias.save(a));
    }
    return result;
  }

  @PreAuthorize("hasRole('ESTUDIANTE')")
  public EntregaTarea entregar(Long id, Requests.EntregaDatos d) {
    Recurso r = recurso(id);
    Tarea t = tareas.findById(id).orElseThrow(ApiException::missing);
    Long section = unidades.findById(r.getIdUnidad()).orElseThrow().getIdSeccion();
    if (access.section(section).getEstado() == SeccionEstado.CERRADA)
      throw ApiException.conflict("La sección está cerrada");
    Matricula m =
        matriculas.findByIdSeccion(section).stream()
            .filter(
                x ->
                    x.getIdUsuario().equals(Actor.actual().id())
                        && x.getEstado() == MatriculaEstado.MATRICULADO)
            .findFirst()
            .orElseThrow(ApiException::denied);
    boolean late = LocalDateTime.now().isAfter(t.getFechaEntrega());
    if (late && !Boolean.TRUE.equals(t.getPermiteTardias()))
      throw ApiException.rule("La tarea no acepta entregas tardías");
    if (d.idArchivo() == null && text.clean(d.comentario()) == null)
      throw ApiException.rule("Adjunte un archivo o comentario");
    if (d.idArchivo() != null) access.ownFile(d.idArchivo());
    if (entregas.findByIdRecurso(id).stream()
        .anyMatch(x -> x.getIdMatricula().equals(m.getIdMatricula())))
      throw ApiException.conflict("Ya entregó esta tarea");
    EntregaTarea e = new EntregaTarea();
    e.setIdRecurso(id);
    e.setIdMatricula(m.getIdMatricula());
    e.setIdArchivo(d.idArchivo());
    e.setComentario(text.clean(d.comentario()));
    e.setFechaEntrega(LocalDateTime.now());
    e.setEstado(late ? EntregaTareaEstado.TARDIA : EntregaTareaEstado.ENTREGADA);
    return entregas.save(e);
  }
}
