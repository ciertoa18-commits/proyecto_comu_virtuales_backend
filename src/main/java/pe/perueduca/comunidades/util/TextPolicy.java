package pe.perueduca.comunidades.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.perueduca.comunidades.exception.ApiException;

@Component
public class TextPolicy {
  private final String[] blocked;

  public TextPolicy(@Value("${app.moderacion.palabras:}") String words) {
    blocked = words.split(",");
  }

  public String clean(String value) {
    if (value == null) return null;
    String s = value.trim();
    if (s.isEmpty()) return null;
    if (s.contains("<") || s.contains(">")) throw ApiException.rule("Solo se admite texto plano");
    for (String w : blocked)
      if (!w.isBlank()
          && java.util.regex.Pattern.compile(
                  "(?iu)\\b" + java.util.regex.Pattern.quote(w.trim()) + "\\b")
              .matcher(s)
              .find()) throw ApiException.rule("El contenido incumple las normas de convivencia");
    return s;
  }

  public String required(String value) {
    String s = clean(value);
    if (s == null) throw ApiException.rule("El contenido no puede estar vacío");
    return s;
  }

  public void url(String value) {
    if (value == null) return;
    try {
      java.net.URI u = java.net.URI.create(value);
      if (!java.util.Set.of("https", "http").contains(u.getScheme())
          || u.getHost() == null
          || u.getUserInfo() != null) throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      throw ApiException.rule("Se requiere una URL HTTP o HTTPS válida");
    }
  }
}
