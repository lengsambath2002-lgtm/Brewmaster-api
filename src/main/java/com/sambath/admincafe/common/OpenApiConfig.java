package com.sambath.admincafe.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI adminCafeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Admin Cafe API")
                .description("Backend for the BrewMaster admin app — categories, products, orders, transactions.")
                .version("v1")
                .license(new License().name("Proprietary")));
    }
}
