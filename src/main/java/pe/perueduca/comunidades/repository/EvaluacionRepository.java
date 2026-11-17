package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface EvaluacionRepository
    extends JpaRepository<Evaluacion, Long>, JpaSpecificationExecutor<Evaluacion> {
  java.util.List<Evaluacion> findByIdSeccion(Long id);

  java.util.List<Evaluacion> findByIdUnidad(Long id);
}
