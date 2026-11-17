package pe.perueduca.comunidades.service;

public interface StorageGateway {
  void put(String key, byte[] bytes, String mime);

  void delete(String key);

  org.springframework.core.io.Resource read(String key);

  default String signedUrl(String key) {
    return null;
  }
}
