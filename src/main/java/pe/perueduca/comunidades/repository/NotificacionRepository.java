package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface NotificacionRepository
    extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {
  java.util.List<Notificacion> findByIdDestinatario(Long id);

  long countByIdDestinatarioAndLeida(Long idDestinatario, Boolean leida);

  java.util.List<Notificacion> findByIdEmisor(Long id);

  java.util.List<Notificacion> findByIdComunidad(Long id);
}
