package pe.perueduca.comunidades.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.Silabo;
import pe.perueduca.comunidades.service.impl.SilaboService;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class SilaboController {
  private final SilaboService service;

  @GetMapping("/secciones/{id}/silabo")
  public ResponseEntity<Silabo> ver(@PathVariable Long id) {
    return service.ver(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PutMapping("/secciones/{id}/silabo")
  public Silabo guardar(@PathVariable Long id, @Valid @RequestBody Requests.SilaboDatos d) {
    return service.guardar(id, d);
  }

  @GetMapping("/me/cursos-resumen")
  public Object misCursos() {
    return service.misCursos();
  }
}
