package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface SeccionRepository
    extends JpaRepository<Seccion, Long>, JpaSpecificationExecutor<Seccion> {
  java.util.List<Seccion> findByIdCurso(Long id);

  java.util.List<Seccion> findByIdPeriodo(Long id);

  java.util.List<Seccion> findByIdDocente(Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select e from Seccion e where e.idSeccion = :id")
  java.util.Optional<Seccion> bloquear(
      @org.springframework.data.repository.query.Param("id") Long id);
}
