package pe.perueduca.comunidades.controller;

import pe.perueduca.comunidades.enums.MiembroComunidadEstado;
import pe.perueduca.comunidades.enums.SolicitudIngresoEstado;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.Actor;
import pe.perueduca.comunidades.service.*;
import pe.perueduca.comunidades.service.impl.*;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class ComunidadController {
  private final ComunidadService service;
  private final AccessService access;
  private final MiembroComunidadRepository miembros;
  private final SolicitudIngresoRepository solicitudes;
  private final UsuarioRepository usuarios;
  private final RolRepository roles;

  @GetMapping("/comunidades")
  public Object list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Long categoria,
      @RequestParam(required = false) Long idioma,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.buscar(q, categoria, idioma, page, size);
  }

  @PostMapping("/comunidades/importar")
  @PreAuthorize("hasRole('ADMIN')")
  public Object importar(@jakarta.validation.Valid @RequestBody Requests.Importacion d) {
    return service.importar(d);
  }

  @GetMapping("/comunidades/recomendadas")
  public Object recommendations() {
    Long yo = Actor.actual().id();
    Long idioma = usuarios.findById(yo).orElseThrow().getIdIdiomaPref();
    return service.recomendadas(yo, idioma, 8);
  }

  @GetMapping("/comunidades/{id}/segundas-lenguas")
  public Object secondLanguages(@PathVariable Long id) {
    return service.segundasLenguas(id);
  }

  @GetMapping("/comunidades/{id}")
  public Object get(@PathVariable Long id) {
    return access.comunidad(id);
  }

  @PostMapping("/comunidades")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object create(@jakarta.validation.Valid @RequestBody Requests.ComunidadDatos d) {
    return service.guardar(null, d);
  }

  @PutMapping("/comunidades/{id}")
  public Object update(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.ComunidadDatos d) {
    return service.guardar(id, d);
  }

  @DeleteMapping("/comunidades/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.desactivar(id);
  }

  @PostMapping("/comunidades/{id}/unirse")
  public Object join(@PathVariable Long id) {
    return service.unirse(id);
  }

  @PostMapping("/comunidades/{id}/solicitudes")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object request(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.Solicitud d) {
    return service.solicitar(id, d);
  }

  @DeleteMapping("/comunidades/{id}/miembros/me")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void leave(@PathVariable Long id) {
    service.salir(id);
  }

  @GetMapping("/comunidades/{id}/miembros")
  @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
  public Object members(@PathVariable Long id) {
    access.member(id);
    return miembros.findByIdComunidad(id).stream()
        .filter(m -> m.getEstado() == MiembroComunidadEstado.ACTIVO)
        .map(
            m -> {
              var u = usuarios.findById(m.getIdUsuario()).orElseThrow();
              return java.util.Map.of(
                  "idUsuario",
                  u.getIdUsuario(),
                  "nombres",
                  u.getNombres(),
                  "apellidos",
                  u.getApellidos(),
                  "rol",
                  roles.findById(u.getIdRol()).orElseThrow().getCodigo(),
                  "rolComunidad",
                  m.getRolComunidad());
            })
        .toList();
  }

  @DeleteMapping("/comunidades/{id}/miembros/{usuario}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void expel(@PathVariable Long id, @PathVariable Long usuario) {
    service.expulsar(id, usuario);
  }

  @GetMapping("/solicitudes")
  @PreAuthorize("hasRole('ADMIN')")
  public Object requests(
      @RequestParam(defaultValue = "PENDIENTE") SolicitudIngresoEstado estado,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    String filtro = q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase() + "%";
    return solicitudes.findAll(
        (r, cq, b) -> {
          jakarta.persistence.criteria.Predicate p = b.equal(r.get("estado"), estado);
          if (filtro == null) return p;
          var u = r.join("refIdUsuario");
          var c = r.join("refIdComunidad");
          return b.and(
              p,
              b.or(
                  b.like(b.lower(u.get("nombres")), filtro),
                  b.like(b.lower(u.get("apellidos")), filtro),
                  b.like(b.lower(u.get("correo")), filtro),
                  b.like(b.lower(c.get("nombre")), filtro)));
        },
        org.springframework.data.domain.PageRequest.of(
            Math.max(page, 0),
            Math.max(1, Math.min(size, 50)),
            org.springframework.data.domain.Sort.by("fechaSolicitud").descending()));
  }

  @PutMapping("/solicitudes/{id}")
  public Object resolve(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.Resolucion d) {
    return service.resolver(id, d);
  }
}
