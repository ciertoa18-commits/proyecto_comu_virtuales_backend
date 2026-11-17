package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface MensajeRepository
    extends JpaRepository<Mensaje, Long>, JpaSpecificationExecutor<Mensaje> {
  java.util.List<Mensaje> findByIdEmisor(Long id);

  java.util.List<Mensaje> findByIdReceptor(Long id);
}
