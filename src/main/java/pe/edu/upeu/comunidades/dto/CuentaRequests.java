package pe.edu.upeu.comunidades.dto;

import jakarta.validation.constraints.*;
import pe.edu.upeu.comunidades.entity.*;

/** Solicitudes de cuenta: registro, recuperación, gestión de usuarios y avisos (iteración 1). */
public final class CuentaRequests {
  private CuentaRequests() {}

  /** Solo correos institucionales; el dominio exacto según el rol lo valida el servicio. */
  public static final String DOMINIO_ESTUDIANTE = "@perueduca.pe";
  public static final String DOMINIO_PERSONAL = "@minedu.gob.pe";
  public static final String CORREO_VALIDO =
      "(?i)^[A-Za-z0-9._%+-]+@(perueduca\\.pe|minedu\\.gob\\.pe)$";
  public static final String MSG_CORREO =
      "debe ser institucional: @perueduca.pe (estudiante) o @minedu.gob.pe (docente/administrador)";

  /** Dominio obligatorio para cada rol. */
  public static String dominioDe(RolCodigo rol) {
    return rol == RolCodigo.ESTUDIANTE ? DOMINIO_ESTUDIANTE : DOMINIO_PERSONAL;
  }

  /** Registro público: solo ESTUDIANTE o DOCENTE. */
  public record Registro(
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidos,
      @NotBlank @Size(max = 150) @Pattern(regexp = CORREO_VALIDO, message = MSG_CORREO)
          String correo,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull RolCodigo rol,
      @Size(max = 100) String especialidad,
      @Pattern(regexp = "^[0-9]{9}$", message = "debe tener 9 dígitos") String telefono,
      Long idIdiomaPref) {}

  public record Recuperacion(@NotBlank @Size(max = 150) String correo) {}

  public record Restablecer(
      @NotBlank @Size(max = 150) String correo,
      @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "debe tener 6 dígitos") String codigo,
      @NotBlank @Size(min = 12, max = 72) String nueva) {}

  /** Alta de usuario hecha por un administrador (cualquier rol, queda ACTIVO). */
  public record NuevoUsuario(
      @NotBlank @Size(max = 100) String nombres,
      @NotBlank @Size(max = 100) String apellidos,
      @NotBlank @Size(max = 150) @Pattern(regexp = CORREO_VALIDO, message = MSG_CORREO)
          String correo,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull RolCodigo rol,
      @Size(max = 100) String especialidad,
      @Pattern(regexp = "^[0-9]{9}$", message = "debe tener 9 dígitos") String telefono) {}

  public record CambioEstado(@NotNull UsuarioEstado estado) {}

  public record ResetPassword(@NotBlank @Size(min = 12, max = 72) String nueva) {}

  public enum Destino {
    TODOS,
    DOCENTES,
    ESTUDIANTES,
    SECCION,
    USUARIO
  }

  public record Aviso(
      @NotNull Destino destino,
      Long idSeccion,
      Long idUsuario,
      @NotBlank @Size(max = 200) String titulo,
      @Size(max = 500) String contenido) {}
}
