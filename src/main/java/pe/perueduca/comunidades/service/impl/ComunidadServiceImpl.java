package pe.perueduca.comunidades.service.impl;

import pe.perueduca.comunidades.enums.ComunidadEstado;
import pe.perueduca.comunidades.enums.ComunidadVisibilidad;
import pe.perueduca.comunidades.enums.MiembroComunidadEstado;
import pe.perueduca.comunidades.enums.MiembroComunidadRolComunidad;
import pe.perueduca.comunidades.enums.NotificacionTipo;
import pe.perueduca.comunidades.enums.SolicitudIngresoEstado;
import pe.perueduca.comunidades.util.TextPolicy;

import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.exception.ApiException;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
@Transactional
public class ComunidadServiceImpl implements pe.perueduca.comunidades.service.ComunidadService {
  private final ComunidadRepository repo;
  private final MiembroComunidadRepository miembros;
  private final SolicitudIngresoRepository solicitudes;
  private final AccessService access;
  private final NotificationService notifications;
  private final TextPolicy text;
  private final CategoriaRepository categorias;
  private final IdiomaRepository idiomas;
  private final PuebloOriginarioRepository pueblos;

  @Transactional(readOnly = true)
  public Page<Comunidad> buscar(String q, Long cat, Long lang, int page, int size) {
    return repo.findAll(
        (r, query, b) -> {
          var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
          p.add(b.equal(r.get("estado"), ComunidadEstado.ACTIVA));
          if (q != null && !q.isBlank())
            p.add(
                b.like(
                    b.lower(r.get("nombre")),
                    "%" + q.toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%",
                    '\\'));
          if (cat != null) p.add(b.equal(r.get("idCategoria"), cat));
          if (lang != null) p.add(b.equal(r.get("idIdioma"), lang));
          return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        },
        PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)), Sort.by("nombre")));
  }

  @PreAuthorize("hasRole('ADMIN')")
  public List<Map<String, Object>> importar(Requests.Importacion d) {
    Map<String, Long> cats = new HashMap<>();
    categorias.findAll().forEach(x -> cats.put(clave(x.getNombre()), x.getIdCategoria()));
    Map<String, Long> langs = new HashMap<>();
    idiomas.findAll().forEach(x -> langs.put(clave(x.getNombre()), x.getIdIdioma()));
    Map<String, Long> towns = new HashMap<>();
    pueblos.findAll().forEach(x -> towns.put(clave(x.getNombre()), x.getIdPueblo()));
    Set<String> existentes = new HashSet<>();
    repo.findAll().forEach(c -> existentes.add(clave(c.getNombre())));
    List<Map<String, Object>> resultado = new ArrayList<>();
    int fila = 1;
    for (Requests.ComunidadImportada x : d.filas()) {
      fila++;
      String nombre = text.clean(x.nombre());
      String error = null;
      Long cat = cats.get(clave(x.categoria()));
      Long lang = vacio(x.idioma()) ? null : langs.get(clave(x.idioma()));
      Long town = vacio(x.pueblo()) ? null : towns.get(clave(x.pueblo()));
      ComunidadVisibilidad vis = visibilidad(x.visibilidad());
      if (nombre == null) error = "Falta el nombre";
      else if (existentes.contains(clave(nombre))) error = "Ya existe una comunidad con ese nombre";
      else if (cat == null) error = "La categoría \"" + x.categoria() + "\" no existe";
      else if (!vacio(x.idioma()) && lang == null) error = "El idioma \"" + x.idioma() + "\" no existe";
      else if (!vacio(x.pueblo()) && town == null) error = "El pueblo \"" + x.pueblo() + "\" no existe";
      else if (vis == null) error = "La visibilidad debe ser PUBLICA o PRIVADA";
      if (error == null) {
        try {
          text.required(nombre);
          text.clean(x.descripcion());
        } catch (ApiException e) {
          error = e.getMessage();
        }
      }
      if (error != null) {
        resultado.add(Map.of("fila", fila, "nombre", nombre == null ? "" : nombre, "ok", false, "mensaje", error));
        continue;
      }
      Comunidad c = new Comunidad();
      c.setNombre(nombre);
      c.setDescripcion(text.clean(x.descripcion()));
      c.setIdCategoria(cat);
      c.setIdIdioma(lang);
      c.setIdPueblo(town);
      c.setVisibilidad(vis);
      c.setModeracionPrevia(false);
      c.setIdCreador(Actor.actual().id());
      c.setEstado(ComunidadEstado.ACTIVA);
      c.setFechaCreacion(LocalDateTime.now());
      repo.save(c);
      existentes.add(clave(nombre));
      resultado.add(Map.of("fila", fila, "nombre", nombre, "ok", true, "mensaje", "Creada"));
    }
    return resultado;
  }

  private static String clave(String s) {
    return s == null
        ? ""
        : java.text.Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
  }

  private static boolean vacio(String s) {
    return s == null || s.isBlank();
  }

  private static ComunidadVisibilidad visibilidad(String s) {
    String v = clave(s).toUpperCase(Locale.ROOT);
    if (v.isEmpty() || v.startsWith("PUB")) return ComunidadVisibilidad.PUBLICA;
    if (v.startsWith("PRIV")) return ComunidadVisibilidad.PRIVADA;
    return null;
  }

  @Transactional(readOnly = true)
  public Page<Comunidad> recomendadas(Long idUsuario, Long idiomaPreferido, int size) {
    Set<Long> excluidas = new HashSet<>();
    List<Comunidad> mias = new ArrayList<>();
    for (MiembroComunidad m : miembros.findByIdUsuario(idUsuario)) {
      if (m.getEstado() != MiembroComunidadEstado.ACTIVO) continue;
      excluidas.add(m.getIdComunidad());
      repo.findById(m.getIdComunidad()).ifPresent(mias::add);
    }
    solicitudes.findByIdUsuario(idUsuario).stream()
        .filter(s -> s.getEstado() == SolicitudIngresoEstado.PENDIENTE)
        .forEach(s -> excluidas.add(s.getIdComunidad()));
    Set<Long> idiomasCercanos = new HashSet<>();
    Set<Long> pueblosCercanos = new HashSet<>();
    Set<Long> categoriasCercanas = new HashSet<>();
    if (idiomaPreferido != null) idiomasCercanos.add(idiomaPreferido);
    for (Comunidad c : mias) {
      if (c.getIdIdioma() != null) idiomasCercanos.add(c.getIdIdioma());
      if (c.getIdPueblo() != null) pueblosCercanos.add(c.getIdPueblo());
      categoriasCercanas.add(c.getIdCategoria());
    }
    List<Comunidad> lista =
        repo.findAll(
                (r, q, b) -> b.equal(r.get("estado"), ComunidadEstado.ACTIVA), Sort.by("nombre"))
            .stream()
            .filter(c -> !excluidas.contains(c.getIdComunidad()))
            .sorted(
                Comparator.comparingInt(
                    (Comunidad c) ->
                        -puntaje(c, idiomaPreferido, idiomasCercanos, pueblosCercanos, categoriasCercanas)))
            .limit(Math.max(1, Math.min(size, 50)))
            .toList();
    return new PageImpl<>(lista, PageRequest.of(0, Math.max(1, lista.size())), lista.size());
  }

  private static int puntaje(
      Comunidad c,
      Long idiomaPreferido,
      Set<Long> idiomas,
      Set<Long> pueblos,
      Set<Long> categorias) {
    int p = 0;
    if (idiomaPreferido != null && idiomaPreferido.equals(c.getIdIdioma())) p += 4;
    else if (c.getIdIdioma() != null && idiomas.contains(c.getIdIdioma())) p += 2;
    if (c.getIdPueblo() != null && pueblos.contains(c.getIdPueblo())) p += 3;
    if (categorias.contains(c.getIdCategoria())) p += 1;
    return p;
  }

  @PreAuthorize("hasRole('ADMIN')")
  public Comunidad guardar(Long id, Requests.ComunidadDatos d) {
    if (!categorias.existsById(d.idCategoria())
        || d.idIdioma() != null && !idiomas.existsById(d.idIdioma())
        || d.idPueblo() != null && !pueblos.existsById(d.idPueblo())) throw ApiException.missing();
    if (d.idImagen() != null) {
      Archivo f = access.ownFile(d.idImagen());
      if (!Set.of("jpg", "jpeg", "png").contains(f.getExtension()))
        throw ApiException.rule("La imagen debe ser JPG o PNG");
    }
    Comunidad c = id == null ? new Comunidad() : access.comunidad(id);
    c.setNombre(text.required(d.nombre()));
    c.setDescripcion(text.clean(d.descripcion()));
    c.setIdCategoria(d.idCategoria());
    c.setIdIdioma(d.idIdioma());
    c.setIdPueblo(d.idPueblo());
    c.setVisibilidad(d.visibilidad());
    c.setModeracionPrevia(d.moderacionPrevia());
    c.setIdImagen(d.idImagen());
    if (id == null) {
      c.setIdCreador(Actor.actual().id());
      c.setEstado(ComunidadEstado.ACTIVA);
      c.setFechaCreacion(LocalDateTime.now());
    } else c.setFechaActualizacion(LocalDateTime.now());
    return repo.save(c);
  }

  @PreAuthorize("hasRole('ADMIN')")
  public void desactivar(Long id) {
    access.comunidad(id).setEstado(ComunidadEstado.INACTIVA);
  }

  private Comunidad locked(Long id) {
    Comunidad c = repo.bloquear(id).orElseThrow(ApiException::missing);
    if (c.getEstado() != ComunidadEstado.ACTIVA) throw ApiException.missing();
    return c;
  }

  private MiembroComunidad activate(Long community, Long user) {
    MiembroComunidad m =
        miembros.findByIdComunidad(community).stream()
            .filter(x -> x.getIdUsuario().equals(user))
            .findFirst()
            .orElseGet(MiembroComunidad::new);
    if (m.getEstado() == MiembroComunidadEstado.EXPULSADO) throw ApiException.denied();
    if (m.getEstado() == MiembroComunidadEstado.ACTIVO)
      throw ApiException.conflict("Ya pertenece a la comunidad");
    m.setIdComunidad(community);
    m.setIdUsuario(user);
    m.setRolComunidad(MiembroComunidadRolComunidad.MIEMBRO);
    m.setEstado(MiembroComunidadEstado.ACTIVO);
    m.setFechaIngreso(LocalDateTime.now());
    m.setFechaSalida(null);
    return miembros.save(m);
  }

  @PreAuthorize("hasAnyRole('ESTUDIANTE','DOCENTE')")
  public MiembroComunidad unirse(Long id) {
    if (locked(id).getVisibilidad() != ComunidadVisibilidad.PUBLICA)
      throw ApiException.rule("La comunidad privada requiere una solicitud aprobada");
    return activate(id, Actor.actual().id());
  }

  @PreAuthorize("hasAnyRole('ESTUDIANTE','DOCENTE')")
  public SolicitudIngreso solicitar(Long id, Requests.Solicitud d) {
    if (locked(id).getVisibilidad() != ComunidadVisibilidad.PRIVADA)
      throw ApiException.rule("La comunidad pública admite ingreso directo");
    if (miembros.findByIdComunidad(id).stream()
        .anyMatch(
            m ->
                m.getIdUsuario().equals(Actor.actual().id())
                    && (m.getEstado() == MiembroComunidadEstado.ACTIVO
                        || m.getEstado() == MiembroComunidadEstado.EXPULSADO)))
      throw ApiException.conflict("No puede solicitar ingreso en su estado actual");
    if (solicitudes.findByIdComunidad(id).stream()
        .anyMatch(
            s ->
                s.getIdUsuario().equals(Actor.actual().id())
                    && s.getEstado() == SolicitudIngresoEstado.PENDIENTE))
      throw ApiException.conflict("Ya existe una solicitud pendiente");
    SolicitudIngreso s = new SolicitudIngreso();
    s.setIdComunidad(id);
    s.setIdUsuario(Actor.actual().id());
    s.setMensaje(text.clean(d.mensaje()));
    s.setEstado(SolicitudIngresoEstado.PENDIENTE);
    s.setFechaSolicitud(LocalDateTime.now());
    s = solicitudes.save(s);
    notifications.solicitud(id, s.getIdSolicitud());
    return s;
  }

  public void salir(Long id) {
    locked(id);
    MiembroComunidad m = access.membership(id);
    if (m.getRolComunidad() == MiembroComunidadRolComunidad.ADMINISTRADOR
        && miembros.findByIdComunidad(id).stream()
                .filter(
                    x ->
                        x.getRolComunidad() == MiembroComunidadRolComunidad.ADMINISTRADOR
                            && x.getEstado() == MiembroComunidadEstado.ACTIVO)
                .count()
            == 1)
      throw ApiException.conflict("Asigne otro administrador antes de abandonar la comunidad");
    m.setEstado(MiembroComunidadEstado.RETIRADO);
    m.setFechaSalida(LocalDateTime.now());
  }

  public void expulsar(Long id, Long user) {
    locked(id);
    access.moderator(id);
    if (user.equals(Actor.actual().id()))
      throw ApiException.rule("Use abandonar comunidad para retirarse");
    MiembroComunidad m =
        miembros.findByIdComunidad(id).stream()
            .filter(
                x ->
                    x.getIdUsuario().equals(user) && x.getEstado() == MiembroComunidadEstado.ACTIVO)
            .findFirst()
            .orElseThrow(ApiException::missing);
    if (!Actor.actual().admin() && m.getRolComunidad() != MiembroComunidadRolComunidad.MIEMBRO)
      throw ApiException.denied();
    m.setEstado(MiembroComunidadEstado.EXPULSADO);
    m.setFechaSalida(LocalDateTime.now());
  }

  @PreAuthorize("hasRole('ADMIN')")
  public SolicitudIngreso resolver(Long id, Requests.Resolucion d) {
    SolicitudIngreso s = solicitudes.bloquear(id).orElseThrow(ApiException::missing);
    locked(s.getIdComunidad());
    if (s.getEstado() != SolicitudIngresoEstado.PENDIENTE)
      throw ApiException.conflict("La solicitud ya fue resuelta");
    if (!d.aprobar() && text.clean(d.motivo()) == null)
      throw ApiException.rule("Indique el motivo de rechazo");
    if (d.aprobar()) activate(s.getIdComunidad(), s.getIdUsuario());
    s.setEstado(d.aprobar() ? SolicitudIngresoEstado.APROBADA : SolicitudIngresoEstado.RECHAZADA);
    s.setIdRevisor(Actor.actual().id());
    s.setFechaRespuesta(LocalDateTime.now());
    s.setMotivoRechazo(d.aprobar() ? null : text.clean(d.motivo()));
    notifications.send(
        s.getIdUsuario(),
        s.getIdComunidad(),
        NotificacionTipo.SOLICITUD_RESPUESTA,
        "Solicitud " + s.getEstado(),
        "SOLICITUD",
        id);
    return s;
  }
}
