package pe.perueduca.comunidades.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.service.impl.CatalogoService;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class CatalogoController {
  private final CategoriaRepository categorias;
  private final IdiomaRepository idiomas;
  private final PuebloOriginarioRepository pueblos;
  private final PeriodoAcademicoRepository periodos;
  private final CatalogoService service;

  @GetMapping("/categorias")
  public Object categorias() {
    return categorias.findAll(Sort.by("nombre"));
  }

  @PostMapping("/categorias")
  @ResponseStatus(HttpStatus.CREATED)
  public Object crearCategoria(@Valid @RequestBody Requests.CategoriaDatos d) {
    return service.guardarCategoria(null, d);
  }

  @PutMapping("/categorias/{id}")
  public Object editarCategoria(@PathVariable Long id, @Valid @RequestBody Requests.CategoriaDatos d) {
    return service.guardarCategoria(id, d);
  }

  @DeleteMapping("/categorias/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarCategoria(@PathVariable Long id) {
    service.eliminarCategoria(id);
  }

  @GetMapping("/idiomas")
  public Object idiomas() {
    return idiomas.findAll(Sort.by("nombre"));
  }

  @PostMapping("/idiomas")
  @ResponseStatus(HttpStatus.CREATED)
  public Object crearIdioma(@Valid @RequestBody Requests.IdiomaDatos d) {
    return service.guardarIdioma(null, d);
  }

  @PutMapping("/idiomas/{id}")
  public Object editarIdioma(@PathVariable Long id, @Valid @RequestBody Requests.IdiomaDatos d) {
    return service.guardarIdioma(id, d);
  }

  @DeleteMapping("/idiomas/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarIdioma(@PathVariable Long id) {
    service.eliminarIdioma(id);
  }

  @GetMapping("/pueblos")
  public Object pueblos() {
    return pueblos.findAll(Sort.by("nombre"));
  }

  @PostMapping("/pueblos")
  @ResponseStatus(HttpStatus.CREATED)
  public Object crearPueblo(@Valid @RequestBody Requests.PuebloDatos d) {
    return service.guardarPueblo(null, d);
  }

  @PutMapping("/pueblos/{id}")
  public Object editarPueblo(@PathVariable Long id, @Valid @RequestBody Requests.PuebloDatos d) {
    return service.guardarPueblo(id, d);
  }

  @DeleteMapping("/pueblos/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarPueblo(@PathVariable Long id) {
    service.eliminarPueblo(id);
  }

  @GetMapping("/periodos")
  public Object periodos() {
    return periodos.findAll();
  }
}
