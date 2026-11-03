package pe.edu.upeu.comunidades.service.impl;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.security.Actor;

@Service
@lombok.RequiredArgsConstructor
public class ArchivoService {
  private final pe.edu.upeu.comunidades.service.StorageGateway storage;
  private final ArchivoRepository repo;
  private final AccessService access;
  private final PublicacionArchivoRepository adjuntos;
  private final PublicacionRepository publicaciones;
  private final RecursoRepository recursos;
  private final UnidadRepository unidades;
  private final UsuarioRepository usuarios;
  private final ComunidadRepository comunidades;
  private final EntregaTareaRepository entregas;

  @Value("${app.storage.directory:./storage}")
  private String directory;

  private static final Map<String, String> MIME =
      Map.of(
          "pdf",
          "application/pdf",
          "mp4",
          "video/mp4",
          "docx",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "jpg",
          "image/jpeg",
          "jpeg",
          "image/jpeg",
          "png",
          "image/png");

  public static void validar(String ext, byte[] bytes, boolean foto) {
    boolean image = Set.of("jpg", "jpeg", "png").contains(ext);
    if (!MIME.containsKey(ext)
        || foto && !image
        || bytes.length == 0
        || bytes.length > (image ? 2097152 : 52428800))
      throw ApiException.rule("Formato o tamaño de archivo no permitido");
    boolean signature =
        switch (ext) {
          case "pdf" -> starts(bytes, new byte[] {37, 80, 68, 70, 45});
          case "png" -> starts(bytes, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
          case "jpg", "jpeg" -> starts(bytes, new byte[] {(byte) 255, (byte) 216, (byte) 255});
          case "mp4" ->
              bytes.length >= 12
                  && new String(bytes, 4, 4, java.nio.charset.StandardCharsets.US_ASCII)
                      .equals("ftyp");
          case "docx" -> docx(bytes);
          default -> false;
        };
    if (!signature)
      throw ApiException.rule("El contenido del archivo no coincide con su extensión");
    if (image) {
      try {
        var img = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null || (long) img.getWidth() * img.getHeight() > 25000000)
          throw ApiException.rule("Imagen inválida o demasiado grande");
      } catch (IOException e) {
        throw ApiException.rule("Imagen inválida");
      }
    }
  }

  private static boolean starts(byte[] b, byte[] p) {
    if (b.length < p.length) return false;
    for (int i = 0; i < p.length; i++) if (b[i] != p[i]) return false;
    return true;
  }

  private static boolean docx(byte[] b) {
    try (var z = new java.util.zip.ZipInputStream(new ByteArrayInputStream(b))) {
      boolean content = false, document = false;
      long total = 0;
      int entries = 0;
      java.util.zip.ZipEntry e;
      byte[] buf = new byte[8192];
      while ((e = z.getNextEntry()) != null) {
        if (++entries > 5000) return false;
        String n = e.getName();
        if (n.equals("[Content_Types].xml")) content = true;
        if (n.equals("word/document.xml")) document = true;
        int read;
        while ((read = z.read(buf)) != -1) {
          total += read;
          if (total > 104857600) return false;
        }
      }
      return content && document;
    } catch (IOException e) {
      return false;
    }
  }

  @Transactional
  public Archivo subir(MultipartFile f, boolean foto) {
    try {
      String original =
          Optional.ofNullable(f.getOriginalFilename()).orElse("archivo").replace('\\', '/');
      original = original.substring(original.lastIndexOf('/') + 1);
      if (original.length() > 255) throw ApiException.rule("Nombre de archivo demasiado largo");
      String ext =
          original.contains(".")
              ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
              : "";
      byte[] bytes = f.getBytes();
      validar(ext, bytes, foto);
      String key = UUID.randomUUID() + "." + ext;
      storage.put(key, bytes, MIME.get(ext));
      Archivo a = new Archivo();
      a.setIdSubidoPor(Actor.actual().id());
      a.setNombreOriginal(original);
      a.setExtension(ext);
      a.setTipoMime(MIME.get(ext));
      a.setTamanoBytes((long) bytes.length);
      a.setRutaAlmacenamiento(key);
      a.setFechaSubida(java.time.LocalDateTime.now());
      org.springframework.transaction.support.TransactionSynchronizationManager
          .registerSynchronization(
              new org.springframework.transaction.support.TransactionSynchronization() {
                public void afterCompletion(int status) {
                  if (status != STATUS_COMMITTED)
                    try {
                      storage.delete(key);
                    } catch (RuntimeException ignored) {
                    }
                }
              });
      return repo.save(a);
    } catch (IOException e) {
      throw new ApiException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo almacenar el archivo");
    }
  }

  public Archivo permitido(Long id) {
    Archivo a = repo.findById(id).orElseThrow(ApiException::missing);
    if (Actor.actual().admin() || a.getIdSubidoPor().equals(Actor.actual().id())) return a;
    for (PublicacionArchivo pa : adjuntos.findByIdArchivo(id)) {
      var p = publicaciones.findById(pa.getIdPublicacion());
      if (p.isPresent()
          && p.get().getEstado() == PublicacionEstado.APROBADA
          && canRead(() -> access.member(p.get().getIdComunidad()))) {
        return a;
      }
    }
    for (Recurso r : recursos.findByIdArchivo(id)) {
      if (canRead(
          () -> access.enrolled(unidades.findById(r.getIdUnidad()).orElseThrow().getIdSeccion())))
        return a;
    }
    for (EntregaTarea e : entregas.findByIdArchivo(id)) {
      var r = recursos.findById(e.getIdRecurso());
      if (r.isPresent()
          && canRead(
              () ->
                  access.teacher(
                      unidades.findById(r.get().getIdUnidad()).orElseThrow().getIdSeccion()))) {
        return a;
      }
    }
    if (usuarios.findAll().stream().anyMatch(u -> id.equals(u.getIdFotoPerfil()))
        || comunidades.findAll().stream()
            .anyMatch(c -> c.getEstado() == ComunidadEstado.ACTIVA && id.equals(c.getIdImagen())))
      return a;
    throw ApiException.denied();
  }

  public String signedUrl(Archivo a) {
    return storage.signedUrl(a.getRutaAlmacenamiento());
  }

  private boolean canRead(Runnable permission) {
    try {
      permission.run();
      return true;
    } catch (ApiException e) {
      if (e.status == org.springframework.http.HttpStatus.FORBIDDEN
          || e.status == org.springframework.http.HttpStatus.NOT_FOUND) return false;
      throw e;
    }
  }

  public org.springframework.core.io.Resource descargar(Archivo a) {
    return storage.read(a.getRutaAlmacenamiento());
  }
}
