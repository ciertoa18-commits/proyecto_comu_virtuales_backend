package pe.perueduca.comunidades.entity;

import pe.perueduca.comunidades.util.BooleanNumericoConverter;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "CODIGO_RECUPERACION")
@Getter
@Setter
public class CodigoRecuperacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_codigo", nullable = false)
  private Long idCodigo;

  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Column(name = "codigo_hash", nullable = false, length = 255)
  private String codigoHash;

  @Column(name = "intentos", nullable = false)
  private Long intentos;

  @Convert(converter = BooleanNumericoConverter.class)
  @Column(name = "usado", nullable = false)
  private Boolean usado;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "fecha_expiracion", nullable = false)
  private LocalDateTime fechaExpiracion;

  public boolean vigente(LocalDateTime ahora) {
    return !Boolean.TRUE.equals(usado) && intentos < 5 && fechaExpiracion.isAfter(ahora);
  }
}
