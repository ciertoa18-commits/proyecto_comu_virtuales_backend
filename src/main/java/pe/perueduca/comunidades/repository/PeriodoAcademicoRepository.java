package pe.perueduca.comunidades.repository;

import org.springframework.data.jpa.repository.*;
import pe.perueduca.comunidades.entity.*;

public interface PeriodoAcademicoRepository
    extends JpaRepository<PeriodoAcademico, Long>, JpaSpecificationExecutor<PeriodoAcademico> {}
