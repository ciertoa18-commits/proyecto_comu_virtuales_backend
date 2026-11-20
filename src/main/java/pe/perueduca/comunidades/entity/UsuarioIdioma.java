package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USUARIO_IDIOMA")
@Getter
@Setter
@NoArgsConstructor
@IdClass(UsuarioIdiomaId.class)
public class UsuarioIdioma {
  @Id
  @Column(name = "id_usuario", nullable = false)
  private Long idUsuario;

  @Id
  @Column(name = "id_idioma", nullable = false)
  private Long idIdioma;

  public UsuarioIdioma(Long idUsuario, Long idIdioma) {
    this.idUsuario = idUsuario;
    this.idIdioma = idIdioma;
  }
}
