package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SILABO")
@Getter
@Setter
public class Silabo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_silabo", nullable = false)
  private Long idSilabo;

  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "sumilla", length = 4000)
  private String sumilla;

  @Column(name = "competencias", length = 4000)
  private String competencias;

  @Column(name = "contenidos", length = 4000)
  private String contenidos;

  @Column(name = "metodologia", length = 4000)
  private String metodologia;

  @Column(name = "evaluacion", length = 4000)
  private String evaluacion;

  @Column(name = "bibliografia", length = 4000)
  private String bibliografia;

  @Column(name = "id_archivo")
  private Long idArchivo;

  @Column(name = "id_autor", nullable = false)
  private Long idAutor;

  @Column(name = "fecha_actualizacion", nullable = false)
  private LocalDateTime fechaActualizacion;
}
