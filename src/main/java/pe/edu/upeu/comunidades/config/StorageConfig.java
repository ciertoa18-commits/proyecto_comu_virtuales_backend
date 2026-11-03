package pe.edu.upeu.comunidades.config;

import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.io.*;
import pe.edu.upeu.comunidades.exception.ApiException;
import pe.edu.upeu.comunidades.service.StorageGateway;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.*;

@Configuration
public class StorageConfig {
  @Bean
  @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
  StorageGateway local(@Value("${app.storage.directory:./storage}") String directory) {
    return new StorageGateway() {
      final Path root = Path.of(directory).toAbsolutePath().normalize();

      Path path(String k) {
        Path p = root.resolve(k).normalize();
        if (!p.startsWith(root)) throw ApiException.denied();
        return p;
      }

      public void put(String k, byte[] b, String mime) {
        try {
          Files.createDirectories(root);
          Files.write(path(k), b, StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException e) {
          throw new ApiException(
              org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
              "Almacenamiento no disponible");
        }
      }

      public void delete(String k) {
        try {
          Files.deleteIfExists(path(k));
        } catch (java.io.IOException e) {
          throw new IllegalStateException(e);
        }
      }

      public Resource read(String k) {
        Path p = path(k);
        if (!Files.isRegularFile(p)) throw ApiException.missing();
        return new FileSystemResource(p);
      }
    };
  }

  @Bean
  @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
  StorageGateway s3(
      @Value("${app.storage.s3.bucket}") String bucket,
      @Value("${app.storage.s3.region:us-east-1}") String region) {
    var client =
        S3Client.builder()
            .region(Region.of(region))
            .httpClientBuilder(
                software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient.builder())
            .build();
    var signer = S3Presigner.builder().region(Region.of(region)).build();
    return new StorageGateway() {
      public void put(String k, byte[] b, String mime) {
        client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(k).contentType(mime).build(),
            software.amazon.awssdk.core.sync.RequestBody.fromBytes(b));
      }

      public void delete(String k) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(k).build());
      }

      public Resource read(String k) {
        throw new IllegalStateException("Use la URL prefirmada");
      }

      public String signedUrl(String k) {
        return signer
            .presignGetObject(
                r ->
                    r.signatureDuration(java.time.Duration.ofMinutes(5))
                        .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(k).build()))
            .url()
            .toString();
      }
    };
  }
}
