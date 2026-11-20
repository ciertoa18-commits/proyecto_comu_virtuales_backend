package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.enums.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.util.TextPolicy;

@ExtendWith(MockitoExtension.class)
class AcademicoServiceTest {

  @Mock CursoRepository cursos;
  @Mock SeccionRepository secciones;
  @Mock UnidadRepository unidades;
  @Mock SesionRepository sesiones;
  @Mock RecursoRepository recursos;
  @Mock TareaRepository tareas;
  @Mock MatriculaRepository matriculas;
  @Mock EvaluacionRepository evaluaciones;
  @Mock NotaRepository notas;
  @Mock AsistenciaRepository asistencias;
  @Mock EntregaTareaRepository entregas;
  @Mock EventoRepository eventos;
  @Mock UsuarioRepository usuarios;
  @Mock RolRepository roles;
  @Mock PeriodoAcademicoRepository periodos;
  @Mock AccessService access;
  @Spy TextPolicy text = new TextPolicy("");
  @Mock NotificationService notifications;
  @Mock AuditService audit;
  @Mock PermisoNotaService permisosNota;
  @Mock MiembroComunidadRepository miembros;

  @InjectMocks AcademicoService service;

  private Curso cursoActivo() {
    Curso c = new Curso();
    c.setIdCurso(10L);
    c.setIdComunidad(1L);
    c.setEstado(CursoEstado.ACTIVO);
    return c;
  }

  private Usuario usuario(Long idRol, UsuarioEstado estado) {
    Usuario u = new Usuario();
    u.setIdUsuario(7L);
    u.setIdRol(idRol);
    u.setEstado(estado);
    return u;
  }

  private Rol rol(RolCodigo codigo) {
    Rol r = new Rol();
    r.setCodigo(codigo);
    return r;
  }

  private Requests.SeccionDatos seccion() {
    return new Requests.SeccionDatos(3L, 7L, "A", 30L, "https://meet.google.com/abc");
  }

  @Test
  void cursoInactivoSeTrataComoNoEncontrado() {
    Curso c = cursoActivo();
    c.setEstado(CursoEstado.INACTIVO);
    when(cursos.findById(10L)).thenReturn(Optional.of(c));

    ApiException e = assertThrows(ApiException.class, () -> service.curso(10L));
    assertEquals(HttpStatus.NOT_FOUND, e.status);
  }

  @Test
  void editarCursoActualizaCodigoYNombre() {
    Curso c = cursoActivo();
    when(cursos.findById(10L)).thenReturn(Optional.of(c));
    when(cursos.save(any(Curso.class))).thenAnswer(i -> i.getArgument(0));

    Curso r =
        service.guardarCurso(
            null, 10L, new Requests.CursoDatos("MAT-101", " Matemática I ", null));

    assertEquals("MAT-101", r.getCodigo());
    assertEquals("Matemática I", r.getNombre());
  }

  @Test
  void eliminarCursoLoDejaInactivo() {
    Curso c = cursoActivo();
    when(cursos.findById(10L)).thenReturn(Optional.of(c));

    service.eliminarCurso(10L);

    assertEquals(CursoEstado.INACTIVO, c.getEstado());
  }

  @Test
  void abrirSeccionConDocenteActivoQuedaAbierta() {
    when(cursos.findById(10L)).thenReturn(Optional.of(cursoActivo()));
    when(usuarios.findById(7L)).thenReturn(Optional.of(usuario(2L, UsuarioEstado.ACTIVO)));
    when(roles.findById(2L)).thenReturn(Optional.of(rol(RolCodigo.DOCENTE)));
    when(periodos.existsById(3L)).thenReturn(true);
    when(secciones.save(any(Seccion.class))).thenAnswer(i -> i.getArgument(0));

    Seccion s = service.crearSeccion(10L, seccion());

    assertEquals(SeccionEstado.ABIERTA, s.getEstado());
    assertEquals(10L, s.getIdCurso());
    assertEquals(7L, s.getIdDocente());
  }

  @Test
  void abrirSeccionConDocenteInactivoEsRechazado() {
    when(cursos.findById(10L)).thenReturn(Optional.of(cursoActivo()));
    when(usuarios.findById(7L)).thenReturn(Optional.of(usuario(2L, UsuarioEstado.INACTIVO)));

    ApiException e =
        assertThrows(ApiException.class, () -> service.crearSeccion(10L, seccion()));

    assertEquals("El titular debe ser un docente activo", e.getMessage());
    verify(secciones, never()).save(any());
  }

  @Test
  void abrirSeccionConUnEstudianteComoTitularEsRechazado() {
    when(cursos.findById(10L)).thenReturn(Optional.of(cursoActivo()));
    when(usuarios.findById(7L)).thenReturn(Optional.of(usuario(3L, UsuarioEstado.ACTIVO)));
    when(roles.findById(3L)).thenReturn(Optional.of(rol(RolCodigo.ESTUDIANTE)));

    assertThrows(ApiException.class, () -> service.crearSeccion(10L, seccion()));
    verify(secciones, never()).save(any());
  }

  @Test
  void abrirSeccionConPeriodoInexistenteEsRechazado() {
    when(cursos.findById(10L)).thenReturn(Optional.of(cursoActivo()));
    when(usuarios.findById(7L)).thenReturn(Optional.of(usuario(2L, UsuarioEstado.ACTIVO)));
    when(roles.findById(2L)).thenReturn(Optional.of(rol(RolCodigo.DOCENTE)));
    when(periodos.existsById(3L)).thenReturn(false);

    assertThrows(ApiException.class, () -> service.crearSeccion(10L, seccion()));
    verify(secciones, never()).save(any());
  }
}
