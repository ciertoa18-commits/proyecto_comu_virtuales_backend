package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface MiembroComunidadRepository
    extends JpaRepository<MiembroComunidad, Long>, JpaSpecificationExecutor<MiembroComunidad> {
  java.util.List<MiembroComunidad> findByIdComunidad(Long id);

  java.util.List<MiembroComunidad> findByIdUsuario(Long id);
}
