package pe.edu.upeu.comunidades.security;

/** Herramienta de consola: no recibe secretos como argumentos ni modifica la BD. */
public final class PasswordHashTool {
  public static void main(String[] args) {
    var console = System.console();
    if (console == null) throw new IllegalStateException("Ejecute desde una terminal interactiva");
    char[] value = console.readPassword("Contraseña a convertir en BCrypt: ");
    try {
      String password = new String(value);
      if (password.length() < 12
          || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
        throw new IllegalArgumentException("Use al menos 12 caracteres y hasta 72 bytes UTF-8");
      console.printf(
          "%s%n",
          new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(password));
    } finally {
      java.util.Arrays.fill(value, '\0');
    }
  }

  private PasswordHashTool() {}
}
