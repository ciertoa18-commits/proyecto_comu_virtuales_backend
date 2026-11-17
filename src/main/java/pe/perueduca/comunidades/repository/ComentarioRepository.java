package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface ComentarioRepository
    extends JpaRepository<Comentario, Long>, JpaSpecificationExecutor<Comentario> {
  java.util.List<Comentario> findByIdPublicacion(Long id);

  java.util.List<Comentario> findByIdRecurso(Long id);

  java.util.List<Comentario> findByIdComentarioPadre(Long id);

  java.util.List<Comentario> findByIdAutor(Long id);

  java.util.List<Comentario> findByIdEliminadoPor(Long id);
}
