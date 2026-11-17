package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.PeriodoAcademicoEstado;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "PERIODO_ACADEMICO")
@Getter
@Setter
public class PeriodoAcademico {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_periodo", nullable = false)
  private Long idPeriodo;

  @Column(name = "nombre", nullable = false, length = 20)
  private String nombre;

  @Column(name = "fecha_inicio", nullable = false)
  private LocalDate fechaInicio;

  @Column(name = "fecha_fin", nullable = false)
  private LocalDate fechaFin;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 12)
  private PeriodoAcademicoEstado estado;
}
