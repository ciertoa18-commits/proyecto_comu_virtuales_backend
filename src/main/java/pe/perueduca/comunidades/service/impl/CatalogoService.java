package pe.perueduca.comunidades.service.impl;

import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.Categoria;
import pe.perueduca.comunidades.entity.Idioma;
import pe.perueduca.comunidades.entity.PuebloOriginario;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.util.TextPolicy;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class CatalogoService {
  private static final String EN_USO = "No se puede eliminar porque está en uso";

  private final CategoriaRepository categorias;
  private final IdiomaRepository idiomas;
  private final PuebloOriginarioRepository pueblos;
  private final ComunidadRepository comunidades;
  private final UsuarioRepository usuarios;
  private final ComunidadIdiomaRepository segundasLenguas;
  private final TextPolicy text;

  @PreAuthorize("hasAnyRole('ADMIN', 'DOCENTE')")
  public Categoria guardarCategoria(Long id, Requests.CategoriaDatos d) {
    if (id != null && !pe.perueduca.comunidades.security.Actor.actual().admin())
      throw ApiException.denied();
    String nombre = text.required(d.nombre());
    categorias.findAll().stream()
        .filter(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getIdCategoria().equals(id))
        .findAny()
        .ifPresent(c -> {
          throw ApiException.conflict("Ya existe una categoría con ese nombre");
        });
    Categoria c = id == null ? new Categoria() : categorias.findById(id).orElseThrow(ApiException::missing);
    c.setNombre(nombre);
    c.setDescripcion(text.clean(d.descripcion()));
    return categorias.save(c);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void eliminarCategoria(Long id) {
    if (!categorias.existsById(id)) throw ApiException.missing();
    if (!comunidades.findByIdCategoria(id).isEmpty()) throw ApiException.conflict(EN_USO);
    categorias.deleteById(id);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Idioma guardarIdioma(Long id, Requests.IdiomaDatos d) {
    String nombre = text.required(d.nombre());
    String codigo = d.codigo().trim().toLowerCase(Locale.ROOT);
    idiomas.findAll().stream()
        .filter(i -> !i.getIdIdioma().equals(id))
        .filter(i -> i.getCodigo().equalsIgnoreCase(codigo) || i.getNombre().equalsIgnoreCase(nombre))
        .findAny()
        .ifPresent(i -> {
          throw ApiException.conflict("Ya existe un idioma con ese código o nombre");
        });
    Idioma i = id == null ? new Idioma() : idiomas.findById(id).orElseThrow(ApiException::missing);
    i.setCodigo(codigo);
    i.setNombre(nombre);
    return idiomas.save(i);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void eliminarIdioma(Long id) {
    if (!idiomas.existsById(id)) throw ApiException.missing();
    if (!comunidades.findByIdIdioma(id).isEmpty()
        || !usuarios.findByIdIdiomaPref(id).isEmpty()
        || segundasLenguas.existsByIdIdioma(id)) throw ApiException.conflict(EN_USO);
    idiomas.deleteById(id);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public PuebloOriginario guardarPueblo(Long id, Requests.PuebloDatos d) {
    String nombre = text.required(d.nombre());
    pueblos.findAll().stream()
        .filter(p -> p.getNombre().equalsIgnoreCase(nombre) && !p.getIdPueblo().equals(id))
        .findAny()
        .ifPresent(p -> {
          throw ApiException.conflict("Ya existe un pueblo originario con ese nombre");
        });
    PuebloOriginario p = id == null ? new PuebloOriginario() : pueblos.findById(id).orElseThrow(ApiException::missing);
    p.setNombre(nombre);
    p.setFamiliaLinguistica(text.clean(d.familiaLinguistica()));
    return pueblos.save(p);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void eliminarPueblo(Long id) {
    if (!pueblos.existsById(id)) throw ApiException.missing();
    if (!comunidades.findByIdPueblo(id).isEmpty()) throw ApiException.conflict(EN_USO);
    pueblos.deleteById(id);
  }
}
