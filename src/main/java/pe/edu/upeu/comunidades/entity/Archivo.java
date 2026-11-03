package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ARCHIVO")
@Getter
@Setter
public class Archivo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_archivo", nullable = false)
  private Long idArchivo;

  @Column(name = "id_subido_por", nullable = false)
  private Long idSubidoPor;

  @Column(name = "nombre_original", nullable = false, length = 255)
  private String nombreOriginal;

  @Column(name = "extension", nullable = false, length = 10)
  private String extension;

  @Column(name = "tipo_mime", nullable = false, length = 100)
  private String tipoMime;

  @Column(name = "tamano_bytes", nullable = false)
  private Long tamanoBytes;

  @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
  private String rutaAlmacenamiento;

  @Column(name = "fecha_subida", nullable = false)
  private LocalDateTime fechaSubida;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_subido_por", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdSubidoPor;
}
