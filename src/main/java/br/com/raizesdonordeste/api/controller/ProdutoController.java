package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.CatalogoDtos.ProdutoRequest;
import br.com.raizesdonordeste.application.dto.CatalogoDtos.ProdutoResponse;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** /produtos - catálogo da rede. */
@RestController
@RequestMapping("/produtos")
@Tag(name = "4. Produtos")
public class ProdutoController {

    private final CatalogoService catalogoService;

    public ProdutoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    @Operation(summary = "Lista produtos ativos (paginado: ?page=1&limit=10)", security = {})
    public Pagina<ProdutoResponse> listar(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit) {
        return catalogoService.listarProdutos(page, limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha produto", security = {})
    public ProdutoResponse buscar(@PathVariable Long id) {
        return catalogoService.buscarProduto(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    @Operation(summary = "Cadastra produto (ADMIN, GERENTE)")
    public ProdutoResponse criar(@Valid @RequestBody ProdutoRequest req, @AuthenticationPrincipal UsuarioLogado usuario) {
        return catalogoService.criarProduto(req, usuario);
    }
}
