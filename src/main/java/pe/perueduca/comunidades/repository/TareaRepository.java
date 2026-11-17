package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface TareaRepository
    extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {
  java.util.List<Tarea> findByIdEvaluacion(Long id);
}
