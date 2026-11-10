# Como testar a API Raízes do Nordeste

Guia rápido para rodar e testar a aplicação em poucos minutos.

## 1. Pré-requisitos

- Java 17 ou superior (`java -version` para conferir)
- IntelliJ IDEA (já traz o Maven) ou Maven 3.9+
- Postman (opcional, para rodar a coleção de testes)

## 2. Iniciar a API

1. Abra a pasta do projeto no IntelliJ e aguarde o download das dependências.
2. Execute a classe `RaizesDoNordesteApplication` (botão ▷ ou **Shift + F10**).
   Pelo terminal: `mvn spring-boot:run`
3. Aguarde no console a mensagem `Started RaizesDoNordesteApplication`.

O banco (H2) é criado automaticamente na pasta `data/`, já com unidades, produtos, estoque e usuários de teste.

## 3. Usuários de teste

| Perfil | E-mail | Senha |
|---|---|---|
| CLIENTE | cliente@raizes.com | Cliente@123 |
| COZINHA | cozinha@raizes.com | Cozinha@123 |
| ATENDENTE | atendente@raizes.com | Atendente@123 |
| GERENTE | gerente@raizes.com | Gerente@123 |
| ADMIN | admin@raizes.com | Admin@123 |

## 4. Teste rápido pelo Swagger

Acesse: **http://localhost:8080/swagger-ui.html**

**4.1 Login** — `POST /auth/login`
```json
{ "email": "cliente@raizes.com", "senha": "Cliente@123" }
```
Copie o valor de `accessToken` (sem aspas), clique em **Authorize** no topo, cole o token e confirme.

**4.2 Criar pedido** — `POST /pedidos` → esperado **201**
```json
{ "canalPedido": "APP", "unidadeId": 1, "itens": [ { "produtoId": 1, "quantidade": 2 } ] }
```

**4.3 Pagar o pedido** — `POST /pagamentos` → esperado **201** (pedido passa para `PAGO`)
```json
{ "pedidoId": 1, "metodo": "PIX" }
```

**4.4 Pagamento recusado** — crie outro pedido e pague com o token de recusa → esperado **402**
```json
{ "pedidoId": 2, "metodo": "CREDITO", "tokenPagamento": "tok_recusado" }
```

**4.5 Atualizar status** — faça login como **cozinha**, troque o token no **Authorize** e use
`PATCH /pedidos/1/status` → esperado **200**
```json
{ "status": "EM_PREPARACAO" }
```

**4.6 Cenários de erro**

| Teste | Como fazer | Esperado |
|---|---|---|
| Sem token | `GET /pedidos` sem Authorize | 401 |
| Perfil sem permissão | `POST /pedidos` logado como cozinha | 403 |
| Canal inválido | `"canalPedido": "DRONE"` | 400 |
| Estoque insuficiente | produto 3, quantidade 50, unidade 1 | 409 |
| Produto inexistente | produtoId 999 | 404 |

## 5. Testes com Postman

1. Importe `postman/RaizesDoNordeste.postman_collection.json`.
2. Clique em **Run** na coleção e depois em **Start run** (desmarque *Stop run if an error occurs*).
3. Resultado esperado: **68 testes aprovados, 0 falhas**.

A coleção espera o banco limpo (veja o item 7).

## 6. Testes automatizados

Com a API parada:
```bash
mvn test
```
Resultado esperado: `Tests run: 13, Failures: 0, Errors: 0` e `BUILD SUCCESS`.

## 7. Reiniciar do zero

1. Pare a API.
2. Apague a pasta `data/`.
3. Inicie a API novamente (o banco é recriado com os dados iniciais).

## Problemas comuns

| Problema | Solução |
|---|---|
| 401 mesmo após login | Refaça o **Authorize**, colando só o token (sem aspas e sem "Bearer") |
| `Port 8080 was already in use` | Há outra instância rodando: pare-a ou use `SERVER_PORT=8081` |
| `Migration checksum mismatch` | Apague a pasta `data/` e reinicie |
| Testes do Postman falhando | Apague a pasta `data/`, reinicie a API e rode a coleção de novo |
