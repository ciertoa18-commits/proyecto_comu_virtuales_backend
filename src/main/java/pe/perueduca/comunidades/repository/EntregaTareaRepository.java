package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface EntregaTareaRepository
    extends JpaRepository<EntregaTarea, Long>, JpaSpecificationExecutor<EntregaTarea> {
  java.util.List<EntregaTarea> findByIdRecurso(Long id);

  java.util.List<EntregaTarea> findByIdMatricula(Long id);

  java.util.List<EntregaTarea> findByIdArchivo(Long id);
}
