package pe.perueduca.comunidades.controller;

import pe.perueduca.comunidades.enums.RolCodigo;
import pe.perueduca.comunidades.enums.UsuarioEstado;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.CuentaRequests;
import pe.perueduca.comunidades.entity.*;
import pe.perueduca.comunidades.service.impl.AdminUsuarioService;

/** Panel del administrador: gestión de usuarios y aprobación de docentes. */
@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@lombok.RequiredArgsConstructor
public class AdminUsuarioController {
  private final AdminUsuarioService service;

  @GetMapping
  public Object listar(
      @RequestParam(required = false) RolCodigo rol,
      @RequestParam(required = false) UsuarioEstado estado,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.listar(rol, estado, q, page, size);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Object crear(@jakarta.validation.Valid @RequestBody CuentaRequests.NuevoUsuario d) {
    return service.crear(d);
  }

  @PutMapping("/{id}/aprobar")
  public Object aprobar(@PathVariable Long id) {
    return service.aprobar(id);
  }

  @PutMapping("/{id}/estado")
  public Object estado(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody CuentaRequests.CambioEstado d) {
    return service.cambiarEstado(id, d.estado());
  }

  @PutMapping("/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(
      @PathVariable Long id,
      @jakarta.validation.Valid @RequestBody CuentaRequests.ResetPassword d) {
    service.resetPassword(id, d.nueva());
  }
}
