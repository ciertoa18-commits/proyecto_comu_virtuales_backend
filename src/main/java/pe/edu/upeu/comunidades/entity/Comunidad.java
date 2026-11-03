package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "COMUNIDAD")
@Getter
@Setter
public class Comunidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "descripcion", length = 1000)
  private String descripcion;

  @Column(name = "id_categoria", nullable = false)
  private Long idCategoria;

  @Column(name = "id_idioma")
  private Long idIdioma;

  @Column(name = "id_pueblo")
  private Long idPueblo;

  @Enumerated(EnumType.STRING)
  @Column(name = "visibilidad", nullable = false, length = 10)
  private ComunidadVisibilidad visibilidad;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "moderacion_previa", nullable = false)
  private Boolean moderacionPrevia;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private ComunidadEstado estado;

  @Column(name = "id_imagen")
  private Long idImagen;

  @Column(name = "id_creador", nullable = false)
  private Long idCreador;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion")
  private LocalDateTime fechaActualizacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_categoria", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Categoria refIdCategoria;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_idioma", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Idioma refIdIdioma;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_pueblo", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private PuebloOriginario refIdPueblo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_imagen", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Archivo refIdImagen;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_creador", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdCreador;
}
