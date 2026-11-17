package pe.perueduca.comunidades.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasRole('ADMIN')")
@lombok.RequiredArgsConstructor
public class ReporteController {
  private final JdbcTemplate jdbc;

  @GetMapping("/idiomas")
  public Object idiomas() {
    return jdbc.queryForList(
        "SELECT i.id_idioma,i.nombre,COUNT(DISTINCT c.id_comunidad) comunidades,COUNT(DISTINCT"
            + " r.id_recurso) recursos FROM idioma i LEFT JOIN comunidad c ON"
            + " c.id_idioma=i.id_idioma AND c.estado='ACTIVA' LEFT JOIN recurso r ON"
            + " r.id_idioma=i.id_idioma AND r.eliminado=0 GROUP BY i.id_idioma,i.nombre ORDER BY"
            + " i.nombre");
  }

  @GetMapping("/docentes")
  public Object docentes(@RequestParam(required = false) Long periodo) {
    String sql =
        "SELECT u.id_usuario,u.nombres,u.apellidos,COUNT(DISTINCT s.id_seccion)"
            + " secciones,COUNT(DISTINCT m.id_matricula) matriculas FROM usuario u JOIN rol r ON"
            + " r.id_rol=u.id_rol LEFT JOIN seccion s ON s.id_docente=u.id_usuario "
            + (periodo == null ? "" : "AND s.id_periodo=? ")
            + "LEFT JOIN matricula m ON m.id_seccion=s.id_seccion AND m.estado<>'RETIRADO' WHERE"
            + " r.codigo='DOCENTE' AND u.estado='ACTIVO' GROUP BY"
            + " u.id_usuario,u.nombres,u.apellidos ORDER BY u.apellidos";
    return periodo == null ? jdbc.queryForList(sql) : jdbc.queryForList(sql, periodo);
  }
}
