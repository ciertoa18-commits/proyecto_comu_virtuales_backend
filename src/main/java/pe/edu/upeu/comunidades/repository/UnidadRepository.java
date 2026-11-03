package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface UnidadRepository
    extends JpaRepository<Unidad, Long>, JpaSpecificationExecutor<Unidad> {
  java.util.List<Unidad> findByIdSeccion(Long id);
}
