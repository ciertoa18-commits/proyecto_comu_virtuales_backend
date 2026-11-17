package pe.perueduca.comunidades.config;

@org.springframework.context.annotation.Configuration
@org.springframework.data.web.config.EnableSpringDataWebSupport(
    pageSerializationMode =
        org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode
            .VIA_DTO)
public class PageConfig {}
