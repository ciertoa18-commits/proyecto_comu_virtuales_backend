package pe.edu.upeu.comunidades.controller;

import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.comunidades.dto.Requests;
import pe.edu.upeu.comunidades.entity.*;
import pe.edu.upeu.comunidades.mapper.PublicacionMapper;
import pe.edu.upeu.comunidades.repository.*;
import pe.edu.upeu.comunidades.service.PublicacionService;
import pe.edu.upeu.comunidades.service.impl.*;

@RestController
@RequestMapping("/api/v1")
@lombok.RequiredArgsConstructor
public class PublicacionController {
  private final PublicacionService service;
  private final PublicacionServiceImpl impl;
  private final PublicacionRepository repo;
  private final ComentarioRepository comentarios;
  private final AccessService access;
  private final PublicacionMapper mapper;

  @GetMapping("/comunidades/{id}/publicaciones")
  public Object wall(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    access.member(id);
    return repo.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("idComunidad"), id),
                    b.equal(r.get("estado"), PublicacionEstado.APROBADA)),
            org.springframework.data.domain.PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(100, size)),
                org.springframework.data.domain.Sort.by("fechaPublicacion").descending()))
        .map(mapper::vista);
  }

  @GetMapping("/comunidades/{id}/publicaciones/pendientes")
  public Object pending(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    access.moderator(id);
    return repo.findAll(
            (r, q, b) ->
                b.and(
                    b.equal(r.get("idComunidad"), id),
                    b.equal(r.get("estado"), PublicacionEstado.PENDIENTE)),
            org.springframework.data.domain.PageRequest.of(
                Math.max(0, page), Math.max(1, Math.min(100, size))))
        .map(mapper::vista);
  }

  @GetMapping("/publicaciones/{id}")
  public Object one(@PathVariable Long id) {
    return mapper.vista(impl.visible(id));
  }

  @PostMapping("/comunidades/{id}/publicaciones")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object create(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.PublicacionDatos d) {
    return mapper.vista(service.guardar(id, null, d));
  }

  @PutMapping("/publicaciones/{id}")
  public Object edit(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.PublicacionDatos d) {
    return mapper.vista(service.guardar(null, id, d));
  }

  @DeleteMapping("/publicaciones/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.eliminar(id);
  }

  @PutMapping("/publicaciones/{id}/moderacion")
  public Object moderate(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.Moderacion d) {
    return mapper.vista(service.moderar(id, d));
  }

  @GetMapping("/publicaciones/{id}/comentarios")
  public Object comments(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    impl.visible(id);
    return comentarios
        .findAll(
            (r, q, b) -> b.equal(r.get("idPublicacion"), id),
            org.springframework.data.domain.PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(100, size)),
                org.springframework.data.domain.Sort.by("fechaComentario")))
        .map(mapper::vista);
  }

  @PostMapping("/publicaciones/{id}/comentarios")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object comment(
      @PathVariable Long id, @jakarta.validation.Valid @RequestBody Requests.ComentarioDatos d) {
    return mapper.vista(service.comentar(id, d));
  }

  @DeleteMapping("/comentarios/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void deleteComment(@PathVariable Long id) {
    service.eliminarComentario(id);
  }
}
