package br.com.raizesdonordeste;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes automatizados de integração (sobem a API inteira com banco H2 em memória,
 * migrations Flyway e seed). Cobrem os cenários obrigatórios do plano de testes:
 * autenticação (200/401/403), validação (400/422), regras do pedido (201/404/409),
 * pagamento mock (aprovado/recusado), atualização de status, filtro por canal e auditoria.
 *
 * Executar: mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:raizes-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
class FluxoPedidoIntegrationTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired
    private MockMvc mvc;

    // ---------------- auxiliares ----------------

    private String login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(corpo(r), "$.accessToken");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private long criarPedido(String token, String json) throws Exception {
        MvcResult r = mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(corpo(r), "$.pedidoId")).longValue();
    }

    private String corpo(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    // ---------------- autenticação / autorização ----------------

    @Test
    @DisplayName("T01 - Login válido retorna JWT")
    void loginValido() throws Exception {
        mvc.perform(post("/auth/login").contentType(JSON).content("{\"email\":\"cliente@raizes.com\",\"senha\":\"Cliente@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.perfil").value("CLIENTE"));
    }

    @Test
    @DisplayName("T02 - Senha errada retorna 401 no formato de erro padrão")
    void loginInvalido() throws Exception {
        mvc.perform(post("/auth/login").contentType(JSON).content("{\"email\":\"cliente@raizes.com\",\"senha\":\"errada123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/auth/login"));
    }

    @Test
    @DisplayName("T03 - Listar pedidos sem token retorna 401")
    void semToken() throws Exception {
        mvc.perform(get("/pedidos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("T04 - Perfil COZINHA não pode criar pedido (403)")
    void perfilSemPermissao() throws Exception {
        String token = login("cozinha@raizes.com", "Cozinha@123");
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"canalPedido\":\"APP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACESSO_NEGADO"));
    }

    // ---------------- validação ----------------

    @Test
    @DisplayName("T05 - Pedido sem canalPedido retorna 422")
    void semCanal() throws Exception {
        String token = login("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"unidadeId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("VALIDACAO_FALHOU"))
                .andExpect(jsonPath("$.details[*].field", hasItem("canalPedido")));
    }

    @Test
    @DisplayName("T06 - canalPedido fora do ENUM retorna 400")
    void canalInvalido() throws Exception {
        String token = login("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"canalPedido\":\"DRONE\",\"unidadeId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALOR_INVALIDO"))
                .andExpect(jsonPath("$.details[0].field").value("canalPedido"));
    }

    @Test
    @DisplayName("T07 - Quantidade negativa retorna 422")
    void quantidadeNegativa() throws Exception {
        String token = login("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"canalPedido\":\"APP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":-2}]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details[0].field").value("itens[0].quantidade"));
    }

    // ---------------- regras de negócio ----------------

    @Test
    @DisplayName("T08 - Produto inexistente retorna 404")
    void produtoInexistente() throws Exception {
        String token = login("cliente@raizes.com", "Cliente@123");
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"canalPedido\":\"APP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":999,\"quantidade\":1}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("PRODUTO_NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("T09 - Estoque insuficiente retorna 409")
    void estoqueInsuficiente() throws Exception {
        String token = login("cliente@raizes.com", "Cliente@123");
        // Seed: Baião de dois (produto 3) tem apenas 2 unidades na unidade 1.
        mvc.perform(post("/pedidos").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"canalPedido\":\"APP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":3,\"quantidade\":50}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ESTOQUE_INSUFICIENTE"))
                .andExpect(jsonPath("$.details[0].field").value("itens[0].quantidade"));
    }

    @Test
    @DisplayName("T10 - Fluxo completo: pedido -> pagamento aprovado -> cozinha -> entregue")
    void fluxoCompleto() throws Exception {
        String cliente = login("cliente@raizes.com", "Cliente@123");
        long pedidoId = criarPedido(cliente,
                "{\"canalPedido\":\"TOTEM\",\"unidadeId\":1,\"itens\":[{\"produtoId\":1,\"quantidade\":2},{\"produtoId\":4,\"quantidade\":1}]}");

        mvc.perform(post("/pagamentos").header("Authorization", bearer(cliente)).contentType(JSON)
                        .content("{\"pedidoId\":" + pedidoId + ",\"metodo\":\"PIX\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.statusPedido").value("PAGO"))
                .andExpect(jsonPath("$.pontosFidelidadeGanhos").value(46)); // 2x18,90 + 9,00 = 46,80

        // Idempotência: pagar de novo não cobra duas vezes.
        mvc.perform(post("/pagamentos").header("Authorization", bearer(cliente)).contentType(JSON)
                        .content("{\"pedidoId\":" + pedidoId + ",\"metodo\":\"PIX\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PEDIDO_JA_PAGO"));

        String cozinha = login("cozinha@raizes.com", "Cozinha@123");
        mvc.perform(patch("/pedidos/" + pedidoId + "/status").header("Authorization", bearer(cozinha)).contentType(JSON)
                        .content("{\"status\":\"EM_PREPARACAO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_PREPARACAO"));
        mvc.perform(patch("/pedidos/" + pedidoId + "/status").header("Authorization", bearer(cozinha)).contentType(JSON)
                        .content("{\"status\":\"PRONTO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PRONTO"));

        String atendente = login("atendente@raizes.com", "Atendente@123");
        mvc.perform(patch("/pedidos/" + pedidoId + "/status").header("Authorization", bearer(atendente)).contentType(JSON)
                        .content("{\"status\":\"ENTREGUE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));

        // Cozinha de OUTRA unidade não enxerga o pedido.
        String cozinhaSalvador = login("cozinha.salvador@raizes.com", "Cozinha@123");
        mvc.perform(get("/pedidos/" + pedidoId).header("Authorization", bearer(cozinhaSalvador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T11 - Pagamento recusado retorna 402 e pedido segue AGUARDANDO_PAGAMENTO")
    void pagamentoRecusado() throws Exception {
        String cliente = login("cliente@raizes.com", "Cliente@123");
        long pedidoId = criarPedido(cliente, "{\"canalPedido\":\"APP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":2,\"quantidade\":1}]}");

        mvc.perform(post("/pagamentos").header("Authorization", bearer(cliente)).contentType(JSON)
                        .content("{\"pedidoId\":" + pedidoId + ",\"metodo\":\"CREDITO\",\"tokenPagamento\":\"tok_recusado\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PAGAMENTO_RECUSADO"));

        mvc.perform(get("/pedidos/" + pedidoId).header("Authorization", bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));

        // A tentativa recusada ficou registrada.
        mvc.perform(get("/pagamentos").param("pedidoId", String.valueOf(pedidoId)).header("Authorization", bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECUSADO"));
    }

    @Test
    @DisplayName("T12 - Filtro por canal e status de transição inválida (409)")
    void filtroPorCanalETransicaoInvalida() throws Exception {
        String cliente = login("joao@raizes.com", "Cliente@123");
        long pedidoId = criarPedido(cliente, "{\"canalPedido\":\"WEB\",\"unidadeId\":2,\"itens\":[{\"produtoId\":4,\"quantidade\":1}]}");

        mvc.perform(get("/pedidos").param("canalPedido", "WEB").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].canalPedido", everyItem(is("WEB"))))
                .andExpect(jsonPath("$.itens[*].pedidoId", hasItem((int) pedidoId)));

        // Pedido não pago não pode ir para PRONTO.
        String cozinhaSalvador = login("cozinha.salvador@raizes.com", "Cozinha@123");
        mvc.perform(patch("/pedidos/" + pedidoId + "/status").header("Authorization", bearer(cozinhaSalvador)).contentType(JSON)
                        .content("{\"status\":\"PRONTO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("TRANSICAO_STATUS_INVALIDA"));
    }

    @Test
    @DisplayName("T13 - Criação de pedido gera registro de auditoria")
    void auditoria() throws Exception {
        String cliente = login("cliente@raizes.com", "Cliente@123");
        long pedidoId = criarPedido(cliente, "{\"canalPedido\":\"PICKUP\",\"unidadeId\":1,\"itens\":[{\"produtoId\":5,\"quantidade\":1}]}");

        String admin = login("admin@raizes.com", "Admin@123");
        mvc.perform(get("/auditoria").param("acao", "PEDIDO_CRIADO").param("limit", "100").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].entidadeId", hasItem((int) pedidoId)));

        // Cliente não acessa auditoria.
        mvc.perform(get("/auditoria").header("Authorization", bearer(cliente)))
                .andExpect(status().isForbidden());
    }
}
