package pe.perueduca.comunidades.service;

import org.springframework.data.domain.Page;
import pe.perueduca.comunidades.dto.Requests;
import pe.perueduca.comunidades.entity.*;

public interface ComunidadService {
  Page<Comunidad> buscar(String q, Long categoria, Long idioma, int page, int size);

  Comunidad guardar(Long id, Requests.ComunidadDatos datos);

  java.util.List<Long> segundasLenguas(Long idComunidad);

  void desactivar(Long id);

  MiembroComunidad unirse(Long id);

  SolicitudIngreso solicitar(Long id, Requests.Solicitud datos);

  void salir(Long id);

  void expulsar(Long id, Long usuario);

  SolicitudIngreso resolver(Long id, Requests.Resolucion datos);
}
