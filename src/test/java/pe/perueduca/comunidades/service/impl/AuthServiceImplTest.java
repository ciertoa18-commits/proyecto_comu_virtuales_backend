package pe.perueduca.comunidades.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.repository.*;
import pe.perueduca.comunidades.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock UsuarioRepository usuarios;
  @Mock RolRepository roles;
  @Mock PasswordEncoder encoder;
  @Mock JwtService jwt;
  @Mock AuditoriaRepository auditoria;

  @InjectMocks AuthServiceImpl auth;

  @Test
  void correoInexistenteNoIniciaSesionYUsaUnHashGeneradoUnaSolaVez() {
    when(usuarios.bloquearPorCorreo("nadie@perueduca.pe")).thenReturn(Optional.empty());
    when(encoder.encode(anyString())).thenReturn("hash-generado");

    var r1 = auth.login(new Requests.Login("nadie@perueduca.pe", "cualquiera"));
    var r2 = auth.login(new Requests.Login(" NADIE@perueduca.pe ", "otra"));

    assertFalse(r1.ok());
    assertFalse(r2.ok());
    verify(encoder, times(1)).encode(anyString());
    verify(encoder, times(2)).matches(anyString(), eq("hash-generado"));
  }
}
