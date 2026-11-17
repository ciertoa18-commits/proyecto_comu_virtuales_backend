package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface NotaRepository extends JpaRepository<Nota, Long>, JpaSpecificationExecutor<Nota> {
  java.util.List<Nota> findByIdMatricula(Long id);

  java.util.List<Nota> findByIdEvaluacion(Long id);

  java.util.List<Nota> findByIdRegistradoPor(Long id);
}
