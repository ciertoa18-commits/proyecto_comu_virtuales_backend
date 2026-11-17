package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface AsistenciaRepository
    extends JpaRepository<Asistencia, Long>, JpaSpecificationExecutor<Asistencia> {
  java.util.List<Asistencia> findByIdSesion(Long id);

  java.util.List<Asistencia> findByIdMatricula(Long id);

  java.util.List<Asistencia> findByIdRegistradoPor(Long id);
}
