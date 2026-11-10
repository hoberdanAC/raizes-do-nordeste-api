package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.FidelidadeDtos.LogAuditoriaResponse;
import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** /auditoria - consulta dos logs de ações sensíveis (somente ADMIN). */
@RestController
@RequestMapping("/auditoria")
@Tag(name = "9. Auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista logs de auditoria (?acao=PEDIDO_CRIADO&page=1&limit=20)")
    public Pagina<LogAuditoriaResponse> listar(@RequestParam(required = false) String acao,
                                               @RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer limit) {
        return auditoriaService.listar(acao, page, limit);
    }
}
