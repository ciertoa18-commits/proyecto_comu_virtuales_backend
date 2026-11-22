package pe.perueduca.comunidades.exception;

import java.sql.SQLException;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  public record Error(String codigo, String mensaje) {}

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Error> api(ApiException e) {
    return ResponseEntity.status(e.status).body(new Error(e.status.name(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Error> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            new Error(
                "VALIDACION",
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .distinct()
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Datos inválidos")));
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  ResponseEntity<Error> malformed(Exception e) {
    return ResponseEntity.badRequest()
        .body(new Error("SOLICITUD_INVALIDA", "Revise el formato y los valores de la solicitud"));
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Error> denied() {
    return ResponseEntity.status(403)
        .body(new Error("PROHIBIDO", "No tiene permiso para esta operación"));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Error> data(DataIntegrityViolationException e) {
    int code = 0;
    for (Throwable c = e; c != null; c = c.getCause())
      if (c instanceof SQLException s) {
        code = s.getErrorCode();
        break;
      }
    int status =
        switch (code) {
          case 1, 2292, 20020, 20021 -> 409;
          case 2291 -> 404;
          default -> 422;
        };
    String msg =
        switch (code) {
          case 1 -> "El registro ya existe";
          case 2291 -> "La referencia indicada no existe";
          case 2292 -> "Existen registros relacionados";
          case 20021 -> "No quedan cupos disponibles";
          default -> "Los datos no cumplen las reglas de integridad";
        };
    return ResponseEntity.status(status).body(new Error("INTEGRIDAD", msg));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<Error> size() {
    return ResponseEntity.status(413)
        .body(new Error("ARCHIVO_GRANDE", "El archivo excede el límite permitido"));
  }

  /** Ruta que no existe: 404 (antes terminaba en 401 y la app cerraba la sesion). */
  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  ResponseEntity<Error> noExiste() {
    return ResponseEntity.status(404)
        .body(new Error("NO_ENCONTRADO", "Esta función no está disponible en el servidor. Reinicia el backend."));
  }

  /** Metodo HTTP que la ruta no acepta: 405 en vez de 401. */
  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  ResponseEntity<Error> metodo() {
    return ResponseEntity.status(405)
        .body(new Error("METODO_NO_PERMITIDO", "Esta función no está disponible en el servidor. Reinicia el backend."));
  }
}
