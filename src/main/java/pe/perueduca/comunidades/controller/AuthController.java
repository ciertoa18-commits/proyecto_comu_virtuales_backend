package pe.perueduca.comunidades.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.service.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@lombok.RequiredArgsConstructor
public class AuthController {
  private final AuthService service;

  @PostMapping("/login")
  public ResponseEntity<?> login(@jakarta.validation.Valid @RequestBody Requests.Login r) {
    var result = service.login(r);
    if (result.ok()) return ResponseEntity.ok(result);
    if ("CUENTA_PENDIENTE".equals(result.motivo()))
      return ResponseEntity.status(403)
          .body(
              java.util.Map.of(
                  "codigo",
                  "CUENTA_PENDIENTE",
                  "mensaje",
                  "Tu cuenta de docente aún no fue aprobada por un administrador"));
    if ("CUENTA_BLOQUEADA".equals(result.motivo()))
      return ResponseEntity.status(423)
          .body(
              java.util.Map.of(
                  "codigo",
                  "CUENTA_BLOQUEADA",
                  "mensaje",
                  "Cuenta bloqueada por 15 minutos tras 5 intentos fallidos"));
    return ResponseEntity.status(401)
            .body(
                java.util.Map.of(
                    "codigo",
                    "CREDENCIALES_INVALIDAS",
                    "mensaje",
                    "Credenciales inválidas o cuenta no disponible"));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
    service.logout(token.substring(7));
    return ResponseEntity.noContent().build();
  }
}
