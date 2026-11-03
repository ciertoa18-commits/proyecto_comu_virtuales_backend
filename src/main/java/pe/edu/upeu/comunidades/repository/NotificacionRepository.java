package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface NotificacionRepository
    extends JpaRepository<Notificacion, Long>, JpaSpecificationExecutor<Notificacion> {
  java.util.List<Notificacion> findByIdDestinatario(Long id);

  java.util.List<Notificacion> findByIdEmisor(Long id);

  java.util.List<Notificacion> findByIdComunidad(Long id);
}
