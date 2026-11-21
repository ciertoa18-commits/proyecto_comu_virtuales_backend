package pe.perueduca.comunidades.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.CuentaRequests;
import pe.perueduca.comunidades.service.impl.CuentaService;

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

  @PostMapping("/verificar-codigo")
  public CuentaService.Resultado verificarCodigo(
      @jakarta.validation.Valid @RequestBody CuentaRequests.VerificarCodigo d) {
    return service.verificarCodigo(d);
  }

  @PostMapping("/restablecer")
  public CuentaService.Resultado restablecer(
      @jakarta.validation.Valid @RequestBody CuentaRequests.Restablecer d) {
    return service.restablecer(d);
  }
}
