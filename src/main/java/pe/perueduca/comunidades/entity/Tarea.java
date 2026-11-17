package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TAREA")
@Getter
@Setter
public class Tarea {
  @Id
  @Column(name = "id_recurso", nullable = false)
  private Long idRecurso;

  @Column(name = "fecha_entrega", nullable = false)
  private LocalDateTime fechaEntrega;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "permite_tardias", nullable = false)
  private Boolean permiteTardias;

  @Column(name = "id_evaluacion")
  private Long idEvaluacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_recurso", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Recurso refIdRecurso;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_evaluacion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Evaluacion refIdEvaluacion;
}
