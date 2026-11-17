package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface CursoRepository
    extends JpaRepository<Curso, Long>, JpaSpecificationExecutor<Curso> {
  java.util.List<Curso> findByIdComunidad(Long id);
}
