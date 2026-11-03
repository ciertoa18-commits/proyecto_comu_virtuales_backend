package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface PublicacionArchivoRepository
    extends JpaRepository<PublicacionArchivo, PublicacionArchivoId>,
        JpaSpecificationExecutor<PublicacionArchivo> {
  java.util.List<PublicacionArchivo> findByIdPublicacion(Long id);

  java.util.List<PublicacionArchivo> findByIdArchivo(Long id);
}
