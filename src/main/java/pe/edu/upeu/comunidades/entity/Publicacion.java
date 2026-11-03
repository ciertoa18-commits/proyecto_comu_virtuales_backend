package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PUBLICACION")
@Getter
@Setter
@org.hibernate.annotations.SQLRestriction("eliminado = 0")
public class Publicacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_publicacion", nullable = false)
  private Long idPublicacion;

  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "id_seccion")
  private Long idSeccion;

  @Column(name = "id_autor", nullable = false)
  private Long idAutor;

  @Column(name = "titulo", length = 200)
  private String titulo;

  @Lob
  @Column(name = "contenido")
  private String contenido;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private PublicacionEstado estado;

  @Column(name = "id_moderador")
  private Long idModerador;

  @Column(name = "fecha_moderacion")
  private LocalDateTime fechaModeracion;

  @Column(name = "fecha_publicacion", nullable = false)
  private LocalDateTime fechaPublicacion;

  @Column(name = "fecha_edicion")
  private LocalDateTime fechaEdicion;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "eliminado", nullable = false)
  private Boolean eliminado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Seccion refIdSeccion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_autor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdAutor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_moderador", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdModerador;
}
