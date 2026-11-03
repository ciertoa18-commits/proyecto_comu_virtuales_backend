package pe.edu.upeu.comunidades.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.service.impl.ArchivoService;

@RestController
@RequestMapping("/api/v1/archivos")
@lombok.RequiredArgsConstructor
public class ArchivoController {
  private final ArchivoService service;

  @GetMapping("/{id}")
  public Object metadata(@PathVariable Long id) {
    return service.permitido(id);
  }

  @PostMapping(consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  public Object upload(@RequestPart("archivo") org.springframework.web.multipart.MultipartFile f) {
    return service.subir(f, false);
  }

  @GetMapping("/{id}/descarga")
  public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
    var a = service.permitido(id);
    String url = service.signedUrl(a);
    if (url != null) return ResponseEntity.status(302).location(java.net.URI.create(url)).build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(a.getTipoMime()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(a.getNombreOriginal(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .body(service.descargar(a));
  }
}
