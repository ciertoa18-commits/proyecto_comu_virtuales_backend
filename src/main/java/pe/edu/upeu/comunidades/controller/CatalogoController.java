package pe.edu.upeu.comunidades.controller;

import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.repository.*;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class CatalogoController {
  private final CategoriaRepository categorias;
  private final IdiomaRepository idiomas;
  private final PuebloOriginarioRepository pueblos;
  private final PeriodoAcademicoRepository periodos;

  @GetMapping("/categorias")
  public Object categorias() {
    return categorias.findAll();
  }

  @GetMapping("/idiomas")
  public Object idiomas() {
    return idiomas.findAll();
  }

  @GetMapping("/pueblos")
  public Object pueblos() {
    return pueblos.findAll();
  }

  @GetMapping("/periodos")
  public Object periodos() {
    return periodos.findAll();
  }
}
