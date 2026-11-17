package pe.perueduca.comunidades.controller;

import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.service.impl.*;

@RestController
@RequestMapping("/api/v1/me")
@lombok.RequiredArgsConstructor
public class PerfilController {
  private final PerfilService service;
  private final ArchivoService archivos;

  @GetMapping
  public Object me() {
    return java.util.Map.of(
        "usuario", service.me(), "rol", pe.perueduca.comunidades.security.Actor.actual().rol());
  }

  @PutMapping
  public Object update(@jakarta.validation.Valid @RequestBody Requests.Perfil d) {
    return service.update(d);
  }

  @PutMapping("/password")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void password(@jakarta.validation.Valid @RequestBody Requests.Password d) {
    service.password(d);
  }

  @PutMapping(value = "/foto", consumes = "multipart/form-data")
  public Object photo(@RequestPart("archivo") org.springframework.web.multipart.MultipartFile f) {
    return service.foto(archivos.subir(f, true).getIdArchivo());
  }
}
