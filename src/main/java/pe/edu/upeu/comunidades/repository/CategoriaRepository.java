package pe.edu.upeu.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.edu.upeu.comunidades.entity.*;

public interface CategoriaRepository
    extends JpaRepository<Categoria, Long>, JpaSpecificationExecutor<Categoria> {}
