package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.RecursoTipo;
import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "RECURSO")
@Getter
@Setter
@org.hibernate.annotations.SQLRestriction("eliminado = 0")
public class Recurso {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_recurso", nullable = false)
  private Long idRecurso;

  @Column(name = "id_unidad", nullable = false)
  private Long idUnidad;

  @Column(name = "id_sesion")
  private Long idSesion;

  @Column(name = "id_autor", nullable = false)
  private Long idAutor;

  @Column(name = "titulo", nullable = false, length = 200)
  private String titulo;

  @Column(name = "descripcion", length = 1000)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 10)
  private RecursoTipo tipo;

  @Column(name = "id_idioma")
  private Long idIdioma;

  @Column(name = "id_archivo")
  private Long idArchivo;

  @Column(name = "url_externa", length = 500)
  private String urlExterna;

  @Column(name = "fecha_publicacion", nullable = false)
  private LocalDateTime fechaPublicacion;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "eliminado", nullable = false)
  private Boolean eliminado;

  @Column(name = "fecha_eliminacion")
  private LocalDateTime fechaEliminacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_unidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Unidad refIdUnidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_sesion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Sesion refIdSesion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_autor", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdAutor;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_idioma", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Idioma refIdIdioma;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_archivo", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Archivo refIdArchivo;
}
