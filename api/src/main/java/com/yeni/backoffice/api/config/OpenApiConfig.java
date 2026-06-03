package com.yeni.backoffice.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yeni Backoffice Portfolio API")
                        .version("v1")
                        .description("Portfolio mock APIs for payment gateway, sales operation and settlement workflows.")
                        .license(new License().name("Portfolio Project")));
    }
}
