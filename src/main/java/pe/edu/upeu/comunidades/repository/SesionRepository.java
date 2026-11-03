package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface SesionRepository
    extends JpaRepository<Sesion, Long>, JpaSpecificationExecutor<Sesion> {
  java.util.List<Sesion> findByIdUnidad(Long id);
}
