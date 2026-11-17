package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.enums.AsistenciaEstado;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ASISTENCIA")
@Getter
@Setter
public class Asistencia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_asistencia", nullable = false)
  private Long idAsistencia;

  @Column(name = "id_sesion", nullable = false)
  private Long idSesion;

  @Column(name = "id_matricula", nullable = false)
  private Long idMatricula;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 12)
  private AsistenciaEstado estado;

  @Column(name = "observacion", length = 300)
  private String observacion;

  @Column(name = "id_registrado_por", nullable = false)
  private Long idRegistradoPor;

  @Column(name = "fecha_registro", nullable = false)
  private LocalDateTime fechaRegistro;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_sesion", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Sesion refIdSesion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_matricula", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Matricula refIdMatricula;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_registrado_por", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdRegistradoPor;
}
