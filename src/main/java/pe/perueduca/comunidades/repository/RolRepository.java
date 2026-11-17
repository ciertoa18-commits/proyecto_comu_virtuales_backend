package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface RolRepository extends JpaRepository<Rol, Long>, JpaSpecificationExecutor<Rol> {}
