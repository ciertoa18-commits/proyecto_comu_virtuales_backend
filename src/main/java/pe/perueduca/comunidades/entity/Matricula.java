package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.MatriculaEstado;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "MATRICULA")
@Getter
@Setter
public class Matricula {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_matricula", nullable = false)
  private Long idMatricula;

  @Column(name = "id_seccion", nullable = false)
  private Long idSeccion;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Column(name = "fecha_matricula", nullable = false)
  private LocalDateTime fechaMatricula;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 12)
  private MatriculaEstado estado;

  @Column(name = "nota_final", precision = 4, scale = 2)
  private BigDecimal notaFinal;

  @Column(name = "fecha_retiro")
  private LocalDateTime fechaRetiro;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_seccion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Seccion refIdSeccion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdUsuario;
}
