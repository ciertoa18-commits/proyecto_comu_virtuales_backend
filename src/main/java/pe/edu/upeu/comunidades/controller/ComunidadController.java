package pe.edu.upeu.comunidades.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;
import pe.edu.upeu.comunidades.service.*;
import pe.edu.upeu.comunidades.service.impl.*;

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

  @GetMapping("/comunidades/recomendadas")
  public Object recommendations() {
    Long idioma = usuarios.findById(Actor.actual().id()).orElseThrow().getIdIdiomaPref();
    return service.buscar(null, null, idioma, 0, 10);
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
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return solicitudes.findAll(
        (r, q, b) -> b.equal(r.get("estado"), estado),
        org.springframework.data.domain.PageRequest.of(
            Math.max(page, 0), Math.max(1, Math.min(size, 100))));
  }

  @PutMapping("/solicitudes/{id}")
  public Object resolve(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.Resolucion d) {
    return service.resolver(id, d);
  }
}
