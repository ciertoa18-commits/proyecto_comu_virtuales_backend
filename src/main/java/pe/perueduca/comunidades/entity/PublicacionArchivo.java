package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PUBLICACION_ARCHIVO")
@Getter
@Setter
@IdClass(PublicacionArchivoId.class)
public class PublicacionArchivo {
  @Id
  @Column(name = "id_publicacion", nullable = false)
  private Long idPublicacion;

  @Id
  @Column(name = "id_archivo", nullable = false)
  private Long idArchivo;

  @Column(name = "orden", nullable = false)
  private Long orden;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_publicacion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Publicacion refIdPublicacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_archivo", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Archivo refIdArchivo;
}
