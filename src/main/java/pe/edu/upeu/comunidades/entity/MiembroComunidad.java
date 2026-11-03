package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "MIEMBRO_COMUNIDAD")
@Getter
@Setter
public class MiembroComunidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_miembro", nullable = false)
  private Long idMiembro;

  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol_comunidad", nullable = false, length = 15)
  private MiembroComunidadRolComunidad rolComunidad;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado", nullable = false, length = 10)
  private MiembroComunidadEstado estado;

  @Column(name = "fecha_ingreso", nullable = false)
  private LocalDateTime fechaIngreso;

  @Column(name = "fecha_salida")
  private LocalDateTime fechaSalida;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_comunidad", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Comunidad refIdComunidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdUsuario;
}
