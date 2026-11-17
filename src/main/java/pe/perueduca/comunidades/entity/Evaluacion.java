package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.EvaluacionTipo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "EVALUACION")
@Getter
@Setter
public class Evaluacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_evaluacion", nullable = false)
  private Long idEvaluacion;

  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "id_unidad")
  private Long idUnidad;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo", nullable = false, length = 15)
  private EvaluacionTipo tipo;

  @Column(name = "peso", nullable = false, precision = 5, scale = 2)
  private BigDecimal peso;

  @Column(name = "fecha")
  private LocalDateTime fecha;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Seccion refIdSeccion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_unidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Unidad refIdUnidad;
}
