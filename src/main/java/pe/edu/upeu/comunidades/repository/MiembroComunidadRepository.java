package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface MiembroComunidadRepository
    extends JpaRepository<MiembroComunidad, Long>, JpaSpecificationExecutor<MiembroComunidad> {
  java.util.List<MiembroComunidad> findByIdComunidad(Long id);

  java.util.List<MiembroComunidad> findByIdUsuario(Long id);
}
