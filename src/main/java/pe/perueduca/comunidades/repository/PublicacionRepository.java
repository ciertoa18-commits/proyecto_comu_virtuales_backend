package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface PublicacionRepository
    extends JpaRepository<Publicacion, Long>, JpaSpecificationExecutor<Publicacion> {
  java.util.List<Publicacion> findByIdComunidad(Long id);

  java.util.List<Publicacion> findByIdSeccion(Long id);

  java.util.List<Publicacion> findByIdAutor(Long id);

  java.util.List<Publicacion> findByIdModerador(Long id);
}
