package pe.perueduca.comunidades.realtime;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TiempoRealService extends TextWebSocketHandler {
  static final String USUARIO = "idUsuario";

  private final Map<Long, Set<WebSocketSession>> sesiones = new ConcurrentHashMap<>();

  @Override
  public void afterConnectionEstablished(WebSocketSession s) {
    Long id = (Long) s.getAttributes().get(USUARIO);
    if (id != null) sesiones.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(s);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession s, CloseStatus status) {
    Long id = (Long) s.getAttributes().get(USUARIO);
    if (id == null) return;
    Set<WebSocketSession> lista = sesiones.get(id);
    if (lista != null) {
      lista.remove(s);
      if (lista.isEmpty()) sesiones.remove(id);
    }
  }

  public void avisar(Long idUsuario, String tipo) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              enviar(idUsuario, tipo);
            }
          });
    } else enviar(idUsuario, tipo);
  }

  private void enviar(Long idUsuario, String tipo) {
    Set<WebSocketSession> lista = sesiones.get(idUsuario);
    if (lista == null) return;
    TextMessage mensaje = new TextMessage("{\"tipo\":\"" + tipo + "\"}");
    for (WebSocketSession s : lista) {
      try {
        synchronized (s) {
          if (s.isOpen()) s.sendMessage(mensaje);
        }
      } catch (IOException e) {
        lista.remove(s);
      }
    }
  }
}
