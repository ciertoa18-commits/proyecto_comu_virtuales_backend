package pe.edu.upeu.comunidades.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "AUDITORIA")
@Getter
@Setter
public class Auditoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_auditoria", nullable = false)
  private Long idAuditoria;

  @Column(name = "tabla", nullable = false, length = 30)
  private String tabla;

  @Column(name = "id_registro", nullable = false)
  private Long idRegistro;

  @Enumerated(EnumType.STRING)
  @Column(name = "accion", nullable = false, length = 10)
  private AuditoriaAccion accion;

  @Column(name = "id_usuario")
  private Long idUsuario;

  @Column(name = "fecha", nullable = false)
  private LocalDateTime fecha;

  @Lob
  @Column(name = "valores_antes")
  private String valoresAntes;

  @Lob
  @Column(name = "valores_despues")
  private String valoresDespues;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
  @com.fasterxml.jackson.annotation.JsonIgnore
  private Usuario refIdUsuario;
}
