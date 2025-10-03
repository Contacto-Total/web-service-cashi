package com.cashi.shared.infrastructure;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cashi API")
                        .description("Sistema de Gestion de Cobranzas")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Contacto Total")
                                .email("soporte@contacto-total.com")));
    }
}
