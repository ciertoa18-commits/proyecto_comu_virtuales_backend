package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface EvaluacionRepository
    extends JpaRepository<Evaluacion, Long>, JpaSpecificationExecutor<Evaluacion> {
  java.util.List<Evaluacion> findByIdSeccion(Long id);

  java.util.List<Evaluacion> findByIdUnidad(Long id);
}
