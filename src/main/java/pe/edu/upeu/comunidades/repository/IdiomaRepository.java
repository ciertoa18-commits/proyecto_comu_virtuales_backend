package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface IdiomaRepository
    extends JpaRepository<Idioma, Long>, JpaSpecificationExecutor<Idioma> {}
