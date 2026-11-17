package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EVENTO")
@Getter
@Setter
public class Evento {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_evento", nullable = false)
  private Long idEvento;

  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "id_seccion")
  private Long idSeccion;

  @Column(name = "id_creador", nullable = false)
  private Long idCreador;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Column(name = "descripcion", length = 1000)
  private String descripcion;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDateTime fechaInicio;

  @Column(name = "fecha_fin")
  private LocalDateTime fechaFin;

  @Column(name = "enlace", length = 500)
  private String enlace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Seccion refIdSeccion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_creador", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdCreador;
}
