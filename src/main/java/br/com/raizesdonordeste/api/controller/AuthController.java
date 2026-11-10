package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.AuthDtos.LoginRequest;
import br.com.raizesdonordeste.application.dto.AuthDtos.LoginResponse;
import br.com.raizesdonordeste.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** /auth - autenticação com emissão de JWT. */
@RestController
@RequestMapping("/auth")
@Tag(name = "1. Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e retorna um JWT", security = {})
    @ApiResponse(responseCode = "200", description = "Token gerado")
    @ApiResponse(responseCode = "401", description = "CREDENCIAIS_INVALIDAS")
    @ApiResponse(responseCode = "422", description = "E-mail/senha ausentes ou e-mail inválido")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }
}
