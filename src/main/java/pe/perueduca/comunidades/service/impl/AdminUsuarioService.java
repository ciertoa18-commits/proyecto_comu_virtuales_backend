package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.AuditoriaAccion;
import pe.perueduca.comunidades.enums.MatriculaEstado;
import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.util.TextPolicy;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.CuentaRequests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;

@Service
@Transactional
@lombok.RequiredArgsConstructor
public class AdminUsuarioService {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final SeccionRepository secciones;
  private final MatriculaRepository matriculas;
  private final CursoRepository cursos;
  private final NotificacionRepository notificaciones;
  private final PasswordEncoder encoder;
  private final TextPolicy text;
  private final AuditService audit;
  private final CuentaService cuentas;

  public record UsuarioResumen(
      Long idUsuario,
      String nombres,
      String apellidos,
      String correo,
      String rol,
      String estado,
      String especialidad,
      String telefono,
      LocalDateTime fechaRegistro,
      boolean bloqueado) {}

  public record SeccionResumen(
      Long idSeccion, String grupo, Long idCurso, String curso, String estado) {}

  private Map<Long, String> codigosRol() {
    Map<Long, String> m = new HashMap<>();
    roles.findAll().forEach(r -> m.put(r.getIdRol(), r.getCodigo().name()));
    return m;
  }

  UsuarioResumen resumen(Usuario u, Map<Long, String> rol) {
    return new UsuarioResumen(
        u.getIdUsuario(),
        u.getNombres(),
        u.getApellidos(),
        u.getCorreo(),
        rol.get(u.getIdRol()),
        u.getEstado().name(),
        u.getEspecialidad(),
        u.getTelefono(),
        u.getFechaRegistro(),
        u.getBloqueadoHasta() != null && u.getBloqueadoHasta().isAfter(LocalDateTime.now()));
  }

  @Transactional(readOnly = true)
  public Page<UsuarioResumen> listar(
      RolCodigo rol, UsuarioEstado estado, String q, int page, int size) {
    Map<Long, String> codigos = codigosRol();
    Long idRol = rol == null ? null : cuentas.idRol(rol);
    String filtro = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
    return usuarios
        .findAll(
            (r, cq, b) -> {
              List<Predicate> p = new ArrayList<>();
              if (idRol != null) p.add(b.equal(r.get("idRol"), idRol));
              if (estado != null) p.add(b.equal(r.get("estado"), estado));
              if (filtro != null)
                p.add(
                    b.or(
                        b.like(b.lower(r.get("nombres")), filtro),
                        b.like(b.lower(r.get("apellidos")), filtro),
                        b.like(r.get("correo"), filtro)));
              return b.and(p.toArray(Predicate[]::new));
            },
            PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by("fechaRegistro").descending()))
        .map(u -> resumen(u, codigos));
  }

  public UsuarioResumen crear(CuentaRequests.NuevoUsuario d) {
    String mail = CuentaService.normalizar(d.correo());
    CuentaService.validarDominio(mail, d.rol());
    if (usuarios.findByCorreo(mail).isPresent())
      throw ApiException.conflict("Ya existe una cuenta con ese correo");
    CuentaService.validarBytes(d.password());
    Usuario u = new Usuario();
    u.setIdRol(cuentas.idRol(d.rol()));
    u.setCorreo(mail);
    u.setNombres(text.required(d.nombres()));
    u.setApellidos(text.required(d.apellidos()));
    u.setEspecialidad(d.rol() == RolCodigo.DOCENTE ? text.clean(d.especialidad()) : null);
    u.setTelefono(CuentaService.blancoANulo(d.telefono()));
    u.setCorreoRecuperacion(
        d.correoRecuperacion() == null || d.correoRecuperacion().isBlank()
            ? null
            : CuentaService.normalizar(d.correoRecuperacion()));
    u.setIdIdiomaPref(1L);
    u.setPasswordHash(encoder.encode(d.password()));
    u.setIntentosFallidos(0L);
    u.setEstado(UsuarioEstado.ACTIVO);
    u.setFechaRegistro(LocalDateTime.now());
    usuarios.save(u);
    audit.record(
        "USUARIO",
        u.getIdUsuario(),
        AuditoriaAccion.INSERT,
        null,
        "{\"rol\":\"" + d.rol() + "\",\"creadoPorAdmin\":true}");
    return resumen(u, codigosRol());
  }

  public UsuarioResumen aprobar(Long id) {
    Usuario u = usuarios.findById(id).orElseThrow(ApiException::missing);
    if (u.getEstado() != UsuarioEstado.PENDIENTE)
      throw ApiException.rule("La cuenta no está pendiente de aprobación");
    u.setEstado(UsuarioEstado.ACTIVO);
    u.setFechaActualizacion(LocalDateTime.now());
    avisar(u, "Tu cuenta fue aprobada", "Ya puedes ingresar a Comunidades Virtuales.");
    audit.record("USUARIO", id, AuditoriaAccion.UPDATE, "{\"estado\":\"PENDIENTE\"}",
        "{\"estado\":\"ACTIVO\"}");
    return resumen(u, codigosRol());
  }

  public UsuarioResumen cambiarEstado(Long id, UsuarioEstado estado) {
    if (estado == UsuarioEstado.PENDIENTE) throw ApiException.rule("Estado no permitido");
    if (id.equals(Actor.actual().id()))
      throw ApiException.rule("No puedes cambiar el estado de tu propia cuenta");
    Usuario u = usuarios.findById(id).orElseThrow(ApiException::missing);
    String antes = u.getEstado().name();
    u.setEstado(estado);
    if (estado == UsuarioEstado.ACTIVO) {
      u.setIntentosFallidos(0L);
      u.setBloqueadoHasta(null);
    }
    u.setFechaActualizacion(LocalDateTime.now());
    audit.record("USUARIO", id, AuditoriaAccion.UPDATE, "{\"estado\":\"" + antes + "\"}",
        "{\"estado\":\"" + estado + "\"}");
    return resumen(u, codigosRol());
  }

  public void resetPassword(Long id, String nueva) {
    CuentaService.validarBytes(nueva);
    Usuario u = usuarios.findById(id).orElseThrow(ApiException::missing);
    u.setPasswordHash(encoder.encode(nueva));
    u.setIntentosFallidos(0L);
    u.setBloqueadoHasta(null);
    u.setFechaActualizacion(LocalDateTime.now());
    audit.record("USUARIO", id, AuditoriaAccion.UPDATE, null, "{\"passwordReseteado\":true}");
  }

  @Transactional(readOnly = true)
  public List<UsuarioResumen> buscar(String q) {
    if (q == null || q.trim().length() < 2) return List.of();
    return listar(null, UsuarioEstado.ACTIVO, q, 0, 20).getContent();
  }

  @Transactional(readOnly = true)
  public List<SeccionResumen> misSecciones() {
    Actor a = Actor.actual();
    List<Seccion> lista =
        a.estudiante()
            ? matriculas.findByIdUsuario(a.id()).stream()
                .filter(m -> m.getEstado() != MatriculaEstado.RETIRADO)
                .map(m -> secciones.findById(m.getIdSeccion()).orElse(null))
                .filter(Objects::nonNull)
                .toList()
            : a.admin() ? secciones.findAll() : secciones.findByIdDocente(a.id());
    return lista.stream()
        .map(
            s ->
                new SeccionResumen(
                    s.getIdSeccion(),
                    s.getGrupo(),
                    s.getIdCurso(),
                    cursos.findById(s.getIdCurso()).map(Curso::getNombre).orElse(""),
                    s.getEstado() == null ? null : s.getEstado().name()))
        .toList();
  }

  private void avisar(Usuario u, String titulo, String contenido) {
    Notificacion n = new Notificacion();
    n.setIdDestinatario(u.getIdUsuario());
    n.setIdEmisor(Actor.actual().id());
    n.setTipo(NotificacionTipo.SISTEMA);
    n.setTitulo(titulo);
    n.setContenido(contenido);
    n.setLeida(false);
    n.setFechaCreacion(LocalDateTime.now());
    notificaciones.save(n);
  }
}
