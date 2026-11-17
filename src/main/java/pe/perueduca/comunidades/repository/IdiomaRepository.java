package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface IdiomaRepository
    extends JpaRepository<Idioma, Long>, JpaSpecificationExecutor<Idioma> {}
