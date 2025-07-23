package binhnvh.vaap.promptbuilder.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prompt Builder API")
                        .version("1.0.0")
                        .description("API for managing and testing LLM prompts")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@company.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://company.com/license")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .addSecurityItem(new SecurityRequirement().addList("UserIdAuth"))
                .components(new Components()
                        .addSecuritySchemes("ApiKeyAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key"))
                        .addSecuritySchemes("UserIdAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-User-Id")));
    }
}
