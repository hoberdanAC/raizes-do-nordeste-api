package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.UsuarioDtos.*;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /usuarios - cadastro, dados próprios e direitos do titular (LGPD). */
@RestController
@RequestMapping("/usuarios")
@Tag(name = "2. Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastro público de cliente (perfil CLIENTE)", security = {})
    @ApiResponse(responseCode = "201", description = "Criado")
    @ApiResponse(responseCode = "409", description = "EMAIL_JA_CADASTRADO")
    @ApiResponse(responseCode = "422", description = "VALIDACAO_FALHOU")
    public UsuarioResponse cadastrar(@Valid @RequestBody CadastroClienteRequest req) {
        return usuarioService.cadastrarCliente(req);
    }

    @PostMapping("/funcionarios")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra funcionário (ADMIN)")
    public UsuarioResponse cadastrarFuncionario(@Valid @RequestBody CadastroFuncionarioRequest req,
                                                @AuthenticationPrincipal UsuarioLogado usuario) {
        return usuarioService.cadastrarFuncionario(req, usuario);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário logado (acesso do titular - LGPD)")
    public UsuarioResponse meusDados(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuarioService.meusDados(usuario);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Anonimiza a conta do cliente (direito de eliminação - LGPD)")
    public void anonimizar(@AuthenticationPrincipal UsuarioLogado usuario) {
        usuarioService.anonimizar(usuario);
    }
}
