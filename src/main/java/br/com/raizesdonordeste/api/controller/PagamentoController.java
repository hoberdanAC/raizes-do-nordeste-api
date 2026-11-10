package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.PedidoDtos.PagamentoRequest;
import br.com.raizesdonordeste.application.dto.PedidoDtos.PagamentoResponse;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** /pagamentos - solicitação de pagamento ao gateway mock e histórico de tentativas. */
@RestController
@RequestMapping("/pagamentos")
@Tag(name = "7. Pagamentos (mock)")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE')")
    @Operation(summary = "Solicita pagamento ao gateway mock. tokenPagamento: 'tok_recusado' ou 'tok_falha' simulam erros")
    @ApiResponse(responseCode = "201", description = "APROVADO - pedido passa para PAGO")
    @ApiResponse(responseCode = "402", description = "PAGAMENTO_RECUSADO - pedido continua AGUARDANDO_PAGAMENTO")
    @ApiResponse(responseCode = "409", description = "PEDIDO_JA_PAGO / PEDIDO_CANCELADO")
    @ApiResponse(responseCode = "503", description = "GATEWAY_INDISPONIVEL")
    public PagamentoResponse pagar(@Valid @RequestBody PagamentoRequest req, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pagamentoService.pagar(req, usuario);
    }

    @GetMapping
    @Operation(summary = "Lista as tentativas de pagamento de um pedido (?pedidoId=1)")
    public List<PagamentoResponse> listar(@RequestParam Long pedidoId, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pagamentoService.listarPorPedido(pedidoId, usuario);
    }
}
