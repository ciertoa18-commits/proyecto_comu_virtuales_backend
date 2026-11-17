package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface SesionRepository
    extends JpaRepository<Sesion, Long>, JpaSpecificationExecutor<Sesion> {
  java.util.List<Sesion> findByIdUnidad(Long id);
}
