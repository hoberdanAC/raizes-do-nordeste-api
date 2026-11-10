package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.CatalogoDtos.*;
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

/** /unidades - unidades da rede e cardápio por unidade. */
@RestController
@RequestMapping("/unidades")
@Tag(name = "3. Unidades e Cardápio")
public class UnidadeController {

    private final CatalogoService catalogoService;

    public UnidadeController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    @Operation(summary = "Lista unidades (paginado)", security = {})
    public Pagina<UnidadeResponse> listar(@RequestParam(required = false) Integer page,
                                          @RequestParam(required = false) Integer limit) {
        return catalogoService.listarUnidades(page, limit);
    }

    @GetMapping("/{id}/cardapio")
    @Operation(summary = "Cardápio da unidade (produtos e disponibilidade)", security = {})
    public CardapioResponse cardapio(@PathVariable Long id) {
        return catalogoService.cardapio(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cria unidade (ADMIN)")
    public UnidadeResponse criar(@Valid @RequestBody UnidadeRequest req, @AuthenticationPrincipal UsuarioLogado usuario) {
        return catalogoService.criarUnidade(req, usuario);
    }
}
