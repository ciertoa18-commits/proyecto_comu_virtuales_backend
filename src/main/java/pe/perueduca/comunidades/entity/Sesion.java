package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SESION")
@Getter
@Setter
public class Sesion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_sesion", nullable = false)
  private Long idSesion;

  @Column(name = "id_unidad", nullable = false)
  private Long idUnidad;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Column(name = "orden", nullable = false)
  private Long orden;

  @Column(name = "fecha_inicio")
  private LocalDateTime fechaInicio;

  @Column(name = "duracion_min")
  private Long duracionMin;

  @Column(name = "enlace_clase", length = 500)
  private String enlaceClase;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_unidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Unidad refIdUnidad;
}
