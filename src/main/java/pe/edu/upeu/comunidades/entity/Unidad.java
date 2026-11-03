package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "UNIDAD")
@Getter
@Setter
public class Unidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_unidad", nullable = false)
  private Long idUnidad;

  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "descripcion", length = 1000)
  private String descripcion;

  @Column(name = "orden", nullable = false)
  private Long orden;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Seccion refIdSeccion;
}
