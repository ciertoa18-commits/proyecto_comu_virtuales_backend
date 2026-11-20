package pe.perueduca.comunidades.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "COMUNIDAD_IDIOMA")
@Getter
@Setter
@NoArgsConstructor
@IdClass(ComunidadIdiomaId.class)
public class ComunidadIdioma {
  @Id
  @Column(name = "id_comunidad", nullable = false)
  private Long idComunidad;

  @Id
  @Column(name = "id_idioma", nullable = false)
  private Long idIdioma;

  public ComunidadIdioma(Long idComunidad, Long idIdioma) {
    this.idComunidad = idComunidad;
    this.idIdioma = idIdioma;
  }
}
