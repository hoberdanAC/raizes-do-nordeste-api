package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.CatalogoDtos.EstoqueResponse;
import br.com.raizesdonordeste.application.dto.CatalogoDtos.MovimentacaoEstoqueRequest;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** /estoque - saldo por unidade e movimentações (entrada/saída). */
@RestController
@RequestMapping("/estoque")
@Tag(name = "5. Estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE','COZINHA')")
    @Operation(summary = "Saldo de estoque da unidade (funcionário só vê a própria unidade)")
    public List<EstoqueResponse> saldo(@RequestParam Long unidadeId, @AuthenticationPrincipal UsuarioLogado usuario) {
        return estoqueService.saldoPorUnidade(unidadeId, usuario);
    }

    @PostMapping("/movimentacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    @Operation(summary = "Registra entrada ou saída de estoque (ADMIN, GERENTE)")
    public EstoqueResponse movimentar(@Valid @RequestBody MovimentacaoEstoqueRequest req,
                                      @AuthenticationPrincipal UsuarioLogado usuario) {
        return estoqueService.movimentar(req, usuario);
    }
}
