package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface RecursoRepository
    extends JpaRepository<Recurso, Long>, JpaSpecificationExecutor<Recurso> {
  java.util.List<Recurso> findByIdUnidad(Long id);

  java.util.List<Recurso> findByIdSesion(Long id);

  java.util.List<Recurso> findByIdAutor(Long id);

  java.util.List<Recurso> findByIdIdioma(Long id);

  java.util.List<Recurso> findByIdArchivo(Long id);
}
