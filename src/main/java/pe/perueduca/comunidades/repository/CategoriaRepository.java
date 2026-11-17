package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface CategoriaRepository
    extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {}
