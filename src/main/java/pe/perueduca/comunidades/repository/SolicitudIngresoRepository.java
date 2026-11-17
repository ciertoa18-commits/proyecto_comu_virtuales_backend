package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface SolicitudIngresoRepository
    extends JpaRepository<SolicitudIngreso, Long>, JpaSpecificationExecutor<SolicitudIngreso> {
  java.util.List<SolicitudIngreso> findByIdComunidad(Long id);

  java.util.List<SolicitudIngreso> findByIdUsuario(Long id);

  java.util.List<SolicitudIngreso> findByIdRevisor(Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select e from SolicitudIngreso e where e.idSolicitud = :id")
  java.util.Optional<SolicitudIngreso> bloquear(
      @org.springframework.data.repository.query.Param("id") Long id);
}
