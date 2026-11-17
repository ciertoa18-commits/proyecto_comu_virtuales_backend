package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface EventoRepository
    extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {
  java.util.List<Evento> findByIdComunidad(Long id);

  java.util.List<Evento> findByIdSeccion(Long id);

  java.util.List<Evento> findByIdCreador(Long id);
}
