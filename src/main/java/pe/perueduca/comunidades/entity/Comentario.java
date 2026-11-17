package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "COMENTARIO")
@Getter
@Setter
@org.hibernate.annotations.SQLRestriction("eliminado = 0")
public class Comentario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_comentario", nullable = false)
  private Long idComentario;

  @Column(name = "id_publicacion")
  private Long idPublicacion;

  @Column(name = "id_recurso")
  private Long idRecurso;

  @Column(name = "id_comentario_padre")
  private Long idComentarioPadre;

  @Column(name = "id_autor", nullable = false)
  private Long idAutor;

  @Column(name = "contenido", nullable = false, length = 2000)
  private String contenido;

  @Column(name = "fecha_comentario", nullable = false)
  private LocalDateTime fechaComentario;

  @Column(name = "fecha_edicion")
  private LocalDateTime fechaEdicion;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "eliminado", nullable = false)
  private Boolean eliminado;

  @Column(name = "id_eliminado_por")
  private Long idEliminadoPor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_publicacion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Publicacion refIdPublicacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_recurso", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Recurso refIdRecurso;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comentario_padre", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comentario refIdComentarioPadre;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_autor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdAutor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_eliminado_por", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdEliminadoPor;
}
