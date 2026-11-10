package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.FidelidadeDtos.*;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.FidelidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /fidelidade - consentimento, saldo, histórico e resgate (somente CLIENTE). */
@RestController
@RequestMapping("/fidelidade")
@PreAuthorize("hasRole('CLIENTE')")
@Tag(name = "8. Fidelidade")
public class FidelidadeController {

    private final FidelidadeService fidelidadeService;

    public FidelidadeController(FidelidadeService fidelidadeService) {
        this.fidelidadeService = fidelidadeService;
    }

    @PutMapping("/consentimento")
    @Operation(summary = "Concede ou revoga consentimento LGPD para o programa")
    public SaldoResponse consentimento(@Valid @RequestBody ConsentimentoRequest req, @AuthenticationPrincipal UsuarioLogado usuario) {
        return fidelidadeService.registrarConsentimento(usuario, req.aceito());
    }

    @GetMapping("/saldo")
    @Operation(summary = "Saldo de pontos")
    public SaldoResponse saldo(@AuthenticationPrincipal UsuarioLogado usuario) {
        return fidelidadeService.saldo(usuario);
    }

    @GetMapping("/historico")
    @Operation(summary = "Extrato de pontos (paginado)")
    public Pagina<TransacaoResponse> historico(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer limit,
                                               @AuthenticationPrincipal UsuarioLogado usuario) {
        return fidelidadeService.historico(usuario, page, limit);
    }

    @PostMapping("/resgates")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Resgata pontos (múltiplos de 100) e gera cupom")
    public ResgateResponse resgatar(@Valid @RequestBody ResgateRequest req, @AuthenticationPrincipal UsuarioLogado usuario) {
        return fidelidadeService.resgatar(usuario, req.pontos());
    }
}
