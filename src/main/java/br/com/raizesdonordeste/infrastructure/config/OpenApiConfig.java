package br.com.raizesdonordeste.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI/Swagger gerada a partir do código.
 * Adiciona o botão "Authorize" para colar o JWT e testar rotas protegidas no próprio Swagger.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Raízes do Nordeste")
                        .description("Backend para pedidos multicanal (APP, TOTEM, BALCAO, PICKUP, WEB), estoque por unidade, "
                                + "pagamento mock, fidelidade e autenticação JWT com perfis.\n\n"
                                + "Erros seguem sempre o formato: {error, message, details[], timestamp, path, requestId}.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
