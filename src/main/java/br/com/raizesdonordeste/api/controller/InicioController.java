package br.com.raizesdonordeste.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Rota inicial de boas-vindas com links para a documentação. */
@RestController
@Tag(name = "0. Início")
public class InicioController {

    @GetMapping("/")
    @Operation(summary = "Boas-vindas e links úteis", security = {})
    public Map<String, String> inicio() {
        return Map.of(
                "mensagem", "Bem-vindo à API do Raízes do Nordeste!",
                "documentacao", "/swagger-ui.html",
                "openapi", "/v3/api-docs");
    }
}
