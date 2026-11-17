package pe.perueduca.comunidades.controller;

import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.service.impl.*;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class ComunicacionController {
  private final NotificationService notifications;
  private final MensajeService mensajes;

  @GetMapping("/me/notificaciones")
  public Object notifications(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return notifications.list(page, size);
  }

  @PutMapping("/notificaciones/{id}/leida")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void read(@PathVariable Long id) {
    notifications.read(id);
  }

  @PutMapping("/me/notificaciones/leidas")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void readAll() {
    notifications.readAll();
  }

  @GetMapping("/me/mensajes")
  public Object messages(
      @RequestParam(required = false) Long con,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return mensajes.list(con, page, size);
  }

  @PostMapping("/mensajes")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object send(@jakarta.validation.Valid @RequestBody Requests.MensajeDatos d) {
    return mensajes.send(d);
  }

  @PutMapping("/mensajes/{id}/leido")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void readMessage(@PathVariable Long id) {
    mensajes.read(id);
  }
}
