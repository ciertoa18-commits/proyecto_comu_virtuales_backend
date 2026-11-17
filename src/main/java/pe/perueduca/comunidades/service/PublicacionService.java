package pe.perueduca.comunidades.service;

import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;

public interface PublicacionService {
  Publicacion guardar(Long comunidad, Long id, Requests.PublicacionDatos datos);

  void eliminar(Long id);

  Publicacion moderar(Long id, Requests.Moderacion datos);

  Comentario comentar(Long id, Requests.ComentarioDatos datos);

  void eliminarComentario(Long id);
}
