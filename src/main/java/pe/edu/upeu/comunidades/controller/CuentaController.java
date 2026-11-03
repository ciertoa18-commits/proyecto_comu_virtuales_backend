package pe.edu.upeu.comunidades.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.dto.CuentaRequests;
import pe.edu.upeu.comunidades.service.impl.CuentaService;

/** Rutas públicas de cuenta: crear cuenta y recuperar contraseña. */
@RestController
@RequestMapping("/api/v1/auth")
@lombok.RequiredArgsConstructor
public class CuentaController {
  private final CuentaService service;

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public CuentaService.Resultado registro(
      @jakarta.validation.Valid @RequestBody CuentaRequests.Registro d) {
    return service.registrar(d);
  }

  @PostMapping("/recuperacion")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CuentaService.Resultado recuperacion(
      @jakarta.validation.Valid @RequestBody CuentaRequests.Recuperacion d) {
    return service.solicitarCodigo(d);
  }

  @PostMapping("/restablecer")
  public CuentaService.Resultado restablecer(
      @jakarta.validation.Valid @RequestBody CuentaRequests.Restablecer d) {
    return service.restablecer(d);
  }
}
