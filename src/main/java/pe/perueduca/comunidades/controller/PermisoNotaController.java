package pe.perueduca.comunidades.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.service.impl.PermisoNotaService;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class PermisoNotaController {
  private final PermisoNotaService service;

  @PostMapping("/secciones/{id}/permisos-nota")
  @ResponseStatus(HttpStatus.CREATED)
  public Object solicitar(@PathVariable Long id, @Valid @RequestBody Requests.PermisoNotaDatos d) {
    return service.solicitar(id, d);
  }

  @GetMapping("/secciones/{id}/permisos-nota/estado")
  public ResponseEntity<PermisoNotaService.VistaPermiso> estado(@PathVariable Long id) {
    return service.estado(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
  }

  @GetMapping("/permisos-nota")
  public Object listar(@RequestParam(defaultValue = "PENDIENTE") String estado) {
    return service.listar(estado);
  }

  @PutMapping("/permisos-nota/{id}")
  public Object resolver(@PathVariable Long id, @Valid @RequestBody Requests.RespuestaPermiso d) {
    return service.resolver(id, d);
  }
}
