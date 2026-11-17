package pe.perueduca.comunidades.mapper;

import java.util.*;
import org.springframework.stereotype.Component;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.repository.*;

@Component
@lombok.RequiredArgsConstructor
public class PublicacionMapper {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PublicacionArchivoRepository adjuntos;

  public record Autor(Long id, String nombres, String apellidos, String rol) {}

  public record PublicacionVista(
      Publicacion publicacion, Autor autor, List<PublicacionArchivo> adjuntos) {}

  public record ComentarioVista(Comentario comentario, Autor autor) {}

  public Autor autor(Long id) {
    Usuario u = usuarios.findById(id).orElseThrow();
    return new Autor(
        id,
        u.getNombres(),
        u.getApellidos(),
        roles.findById(u.getIdRol()).orElseThrow().getCodigo().name());
  }

  public PublicacionVista vista(Publicacion p) {
    return new PublicacionVista(
        p, autor(p.getIdAutor()), adjuntos.findByIdPublicacion(p.getIdPublicacion()));
  }

  public ComentarioVista vista(Comentario c) {
    return new ComentarioVista(c, autor(c.getIdAutor()));
  }
}
