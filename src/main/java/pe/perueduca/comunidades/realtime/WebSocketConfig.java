package pe.perueduca.comunidades.realtime;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import pe.perueduca.comunidades.enums.UsuarioEstado;
import pe.perueduca.comunidades.repository.UsuarioRepository;
import pe.perueduca.comunidades.security.JwtService;

@Configuration
@EnableWebSocket
@lombok.RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
  private final TiempoRealService handler;
  private final JwtService jwt;
  private final UsuarioRepository usuarios;

  @Value("${app.cors.origins}")
  private String origenes;

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(handler, "/ws")
        .addInterceptors(new Autenticacion())
        .setAllowedOrigins(origenes.split(","));
  }

  private class Autenticacion implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes) {
      String token =
          UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
      if (token == null || token.isBlank()) return false;
      try {
        String correo = jwt.parse(token).getSubject();
        return usuarios
            .findByCorreo(correo)
            .filter(u -> u.getEstado() == UsuarioEstado.ACTIVO)
            .map(
                u -> {
                  attributes.put(TiempoRealService.USUARIO, u.getIdUsuario());
                  return true;
                })
            .orElse(false);
      } catch (RuntimeException e) {
        return false;
      }
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception) {}
  }
}
