package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "SECCION")
@Getter
@Setter
public class Seccion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "id_curso", nullable = false)
  private Long idCurso;

  @Column(name = "id_periodo", nullable = false)
  private Long idPeriodo;

  @Column(name = "id_docente", nullable = false)
  private Long idDocente;

  @Column(name = "grupo", nullable = false, length = 10)
  private String grupo;

  @Column(name = "cupo_maximo")
  private Long cupoMaximo;

  @Column(name = "sala_virtual", length = 500)
  private String salaVirtual;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private SeccionEstado estado;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_curso", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Curso refIdCurso;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_periodo", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private PeriodoAcademico refIdPeriodo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_docente", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdDocente;
}
