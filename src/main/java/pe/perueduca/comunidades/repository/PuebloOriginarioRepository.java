package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface PuebloOriginarioRepository
    extends JpaRepository<PuebloOriginario, Long>, JpaSpecificationExecutor<PuebloOriginario> {}
