package br.com.raizesdonordeste.api.controller;

import br.com.raizesdonordeste.application.dto.Pagina;
import br.com.raizesdonordeste.application.dto.PedidoDtos.*;
import br.com.raizesdonordeste.application.dto.UsuarioLogado;
import br.com.raizesdonordeste.application.service.PedidoService;
import br.com.raizesdonordeste.domain.enums.CanalPedido;
import br.com.raizesdonordeste.domain.enums.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * /pedidos - fluxo crítico (criar, listar com filtros, consultar, mudar status e cancelar).
 * O controller só traduz HTTP <-> caso de uso; as regras ficam no PedidoService e no domínio.
 */
@RestController
@RequestMapping("/pedidos")
@Tag(name = "6. Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE')")
    @Operation(summary = "Cria pedido (canalPedido obrigatório; valida estoque e calcula total)")
    @ApiResponse(responseCode = "201", description = "Pedido criado com status AGUARDANDO_PAGAMENTO")
    @ApiResponse(responseCode = "200", description = "Idempotency-Key repetida: devolve o pedido já criado")
    @ApiResponse(responseCode = "400", description = "canalPedido com valor fora do ENUM / JSON inválido")
    @ApiResponse(responseCode = "401", description = "Sem token")
    @ApiResponse(responseCode = "403", description = "Perfil sem permissão")
    @ApiResponse(responseCode = "404", description = "Unidade ou produto inexistente")
    @ApiResponse(responseCode = "409", description = "ESTOQUE_INSUFICIENTE")
    @ApiResponse(responseCode = "422", description = "Campo obrigatório ausente / quantidade inválida / canal incompatível")
    public ResponseEntity<PedidoResponse> criar(
            @Valid @RequestBody CriarPedidoRequest req,
            @Parameter(description = "Chave opcional para evitar pedido duplicado em reenvios")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UsuarioLogado usuario) {
        ResultadoCriacaoPedido resultado = pedidoService.criar(req, idempotencyKey, usuario);
        HttpStatus status = resultado.reaproveitado() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado.pedido());
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtros (?canalPedido=TOTEM&status=PAGO&unidadeId=1&page=1&limit=10)")
    public Pagina<PedidoResumoResponse> listar(
            @RequestParam(required = false) CanalPedido canalPedido,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) Long unidadeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.listar(canalPedido, status, unidadeId, page, limit, usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta pedido e status (cliente só vê o próprio)")
    public PedidoResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.buscar(id, usuario);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('COZINHA','ATENDENTE','GERENTE','ADMIN')")
    @Operation(summary = "Atualiza status: COZINHA (EM_PREPARACAO, PRONTO), ATENDENTE (ENTREGUE)")
    @ApiResponse(responseCode = "409", description = "TRANSICAO_STATUS_INVALIDA")
    public PedidoResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest req,
                                          @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.atualizarStatus(id, req.status(), usuario);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasAnyRole('CLIENTE','ATENDENTE','GERENTE','ADMIN')")
    @Operation(summary = "Cancela pedido (devolve estoque; se pago, estorna pagamento e pontos)")
    public PedidoResponse cancelar(@PathVariable Long id,
                                   @Valid @RequestBody(required = false) CancelarPedidoRequest req,
                                   @AuthenticationPrincipal UsuarioLogado usuario) {
        return pedidoService.cancelar(id, req != null ? req.motivo() : null, usuario);
    }
}
