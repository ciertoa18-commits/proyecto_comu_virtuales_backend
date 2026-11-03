package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "NOTA")
@Getter
@Setter
public class Nota {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_nota", nullable = false)
  private Long idNota;

  @Column(name = "id_matricula", nullable = false)
  private Long idMatricula;

  @Column(name = "id_evaluacion", nullable = false)
  private Long idEvaluacion;

  @Column(name = "calificacion", nullable = false, precision = 4, scale = 2)
  private BigDecimal calificacion;

  @Column(name = "comentario_docente", length = 1000)
  private String comentarioDocente;

  @Column(name = "id_registrado_por", nullable = false)
  private Long idRegistradoPor;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  @Column(name = "fecha_modificacion")
  private LocalDateTime fechaModificacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_matricula", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Matricula refIdMatricula;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_evaluacion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Evaluacion refIdEvaluacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_registrado_por", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdRegistradoPor;
}
