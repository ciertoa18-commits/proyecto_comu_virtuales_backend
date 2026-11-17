package pe.perueduca.comunidades.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  public final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException missing() {
    return new ApiException(HttpStatus.NOT_FOUND, "Recurso no encontrado");
  }

  public static ApiException denied() {
    return new ApiException(HttpStatus.FORBIDDEN, "No tiene permiso para esta operación");
  }

  public static ApiException rule(String msg) {
    return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, msg);
  }

  public static ApiException conflict(String msg) {
    return new ApiException(HttpStatus.CONFLICT, msg);
  }
}
