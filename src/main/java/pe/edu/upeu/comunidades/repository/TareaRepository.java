package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface TareaRepository
    extends JpaRepository<Tarea, Long>, JpaSpecificationExecutor<Tarea> {
  java.util.List<Tarea> findByIdEvaluacion(Long id);
}
