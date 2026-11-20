package pe.perueduca.comunidades.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {
  private static final Logger log = LoggerFactory.getLogger(CorreoService.class);
  private final ObjectProvider<JavaMailSender> sender;
  private final String remitente;
  private final boolean smtpConfigurado;

  public CorreoService(
      ObjectProvider<JavaMailSender> sender,
      @Value("${app.mail.from:${spring.mail.username:no-responder@perueduca.pe}}") String remitente,
      @Value("${spring.mail.host:}") String host) {
    this.sender = sender;
    this.remitente = remitente;
    this.smtpConfigurado = !host.isBlank();
  }

  public boolean configurado() {
    return smtpConfigurado && sender.getIfAvailable() != null;
  }

  public void enviar(String para, String asunto, String texto) {
    JavaMailSender mail = smtpConfigurado ? sender.getIfAvailable() : null;
    if (mail == null) {
      log.warn(
          "\n===== CORREO (modo desarrollo, SMTP sin configurar) =====\nPara: {}\nAsunto: {}\n{}\n"
              + "==========================================================",
          para,
          asunto,
          texto);
      return;
    }
    try {
      SimpleMailMessage m = new SimpleMailMessage();
      m.setFrom(remitente);
      m.setTo(para);
      m.setSubject(asunto);
      m.setText(texto);
      mail.send(m);
    } catch (RuntimeException e) {
      log.error("No se pudo enviar el correo a {}: {}", para, e.getMessage());
    }
  }
}
