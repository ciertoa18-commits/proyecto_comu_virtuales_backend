package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.CursoEstado;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CURSO")
@Getter
@Setter
public class Curso {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_curso", nullable = false)
  private Long idCurso;

  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "codigo", nullable = false, length = 20)
  private String codigo;

  @Column(name = "nombre", nullable = false, length = 150)
  private String nombre;

  @Column(name = "descripcion", length = 1000)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private CursoEstado estado;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;
}
