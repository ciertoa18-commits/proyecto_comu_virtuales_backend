package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ENTREGA_TAREA")
@Getter
@Setter
public class EntregaTarea {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_entrega", nullable = false)
  private Long idEntrega;

  @Column(name = "id_recurso", nullable = false)
  private Long idRecurso;

  @Column(name = "id_matricula", nullable = false)
  private Long idMatricula;

  @Column(name = "id_archivo")
  private Long idArchivo;

  @Column(name = "comentario", length = 1000)
  private String comentario;

  @Column(name = "fecha_entrega", nullable = false)
  private LocalDateTime fechaEntrega;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private EntregaTareaEstado estado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_recurso", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Tarea refIdRecurso;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_matricula", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Matricula refIdMatricula;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_archivo", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Archivo refIdArchivo;
}
