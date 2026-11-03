package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface PuebloOriginarioRepository
    extends JpaRepository<PuebloOriginario, Long>, JpaSpecificationExecutor<PuebloOriginario> {}
