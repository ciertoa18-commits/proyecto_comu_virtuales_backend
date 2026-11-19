package pe.perueduca.comunidades.dto;

import pe.perueduca.comunidades.enums.AsistenciaEstado;
import pe.perueduca.comunidades.enums.ComunidadVisibilidad;
import pe.perueduca.comunidades.enums.EvaluacionTipo;
import pe.perueduca.comunidades.enums.PublicacionEstado;
import pe.perueduca.comunidades.enums.RecursoTipo;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import pe.perueduca.comunidades.entity.*;

public final class Requests {
  private Requests() {}

  public record Login(
      @NotBlank @Email @Size(max = 150) String correo,
      @NotBlank @Size(max = 200) String password) {}

  public record Perfil(
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidos,
      @Size(max = 100) String especialidad,
      Long idIdiomaPref,
      @Pattern(regexp = "^[0-9]{9}$", message = "debe tener 9 dígitos") String telefono,
      @Email @Size(max = 150) String correoRecuperacion) {}

  public record Password(
      @NotBlank String actual, @NotBlank @Size(min = 12, max = 72) String nueva) {}

  public record ComunidadDatos(
      @NotBlank @Size(max = 150) String nombre,
      @Size(max = 1000) String descripcion,
      @NotNull Long idCategoria,
      Long idIdioma,
      Long idPueblo,
      @NotNull ComunidadVisibilidad visibilidad,
      boolean moderacionPrevia,
      Long idImagen) {}

  public record Solicitud(@Size(max = 500) String mensaje) {}

  public record Resolucion(boolean aprobar, @Size(max = 500) String motivo) {}

  public record PublicacionDatos(
      @Size(max = 200) String titulo,
      @Size(max = 20000) String contenido,
      Long idSeccion,
      @Size(max = 10) List<@NotNull Long> archivos) {}

  public record Moderacion(@NotNull PublicacionEstado estado) {}

  public record ComentarioDatos(
      @NotBlank @Size(max = 2000) String contenido, Long idComentarioPadre) {}

  public record MensajeDatos(
      @NotNull Long idReceptor, @NotBlank @Size(max = 4000) String contenido) {}

  public record CursoDatos(
      @NotBlank @Size(max = 20) String codigo,
      @NotBlank @Size(max = 150) String nombre,
      @Size(max = 1000) String descripcion) {}

  public record SeccionDatos(
      @NotNull Long idPeriodo,
      @NotNull Long idDocente,
      @NotBlank @Size(max = 10) String grupo,
      @Positive Long cupoMaximo,
      @Size(max = 500) String salaVirtual) {}

  public record UnidadDatos(
      @NotBlank @Size(max = 150) String nombre,
      @Size(max = 1000) String descripcion,
      @NotNull @Positive Long orden) {}

  public record SesionDatos(
      @NotBlank @Size(max = 200) String titulo,
      @NotNull @Positive Long orden,
      LocalDateTime fechaInicio,
      @Positive Long duracionMin,
      @Size(max = 500) String enlaceClase) {}

  public record RecursoDatos(
      @NotBlank @Size(max = 200) String titulo,
      @Size(max = 1000) String descripcion,
      @NotNull RecursoTipo tipo,
      Long idSesion,
      Long idIdioma,
      Long idArchivo,
      @Size(max = 500) String urlExterna,
      LocalDateTime fechaEntrega,
      Boolean permiteTardias,
      Long idEvaluacion) {
    public RecursoDatos {
      permiteTardias = Boolean.TRUE.equals(permiteTardias);
    }
  }

  public record EventoDatos(
      @NotBlank @Size(max = 200) String titulo,
      @Size(max = 1000) String descripcion,
      @NotNull LocalDateTime fechaInicio,
      LocalDateTime fechaFin,
      @Size(max = 500) String enlace,
      Long idSeccion) {}

  public record EvaluacionDatos(
      @NotBlank @Size(max = 100) String nombre,
      @NotNull EvaluacionTipo tipo,
      @NotNull @DecimalMin("0.01") @DecimalMax("100") BigDecimal peso,
      Long idUnidad,
      LocalDateTime fecha) {}

  public record NotaDatos(
      @NotNull Long idMatricula,
      @NotNull Long idEvaluacion,
      @NotNull @DecimalMin("0") @DecimalMax("20") BigDecimal calificacion,
      @Size(max = 1000) String comentarioDocente) {}

  public record AsistenciaDatos(
      @NotNull Long idMatricula,
      @NotNull AsistenciaEstado estado,
      @Size(max = 300) String observacion) {}

  public record EntregaDatos(Long idArchivo, @Size(max = 1000) String comentario) {}
}
