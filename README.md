# API Raízes do Nordeste — Back-end (Java / Spring Boot)

API REST para a rede de lanchonetes **Raízes do Nordeste**: pedidos multicanal (APP, TOTEM, BALCAO, PICKUP, WEB), estoque por unidade, pagamento via gateway **mock**, programa de fidelidade com consentimento, autenticação **JWT** com perfis e auditoria de ações sensíveis.

Desenvolvido em Java 17 + Spring Boot 3. Fluxo crítico entregue: **Fluxo A — Pedido → Pagamento mock → Atualização de status**.

- Repositório: https://github.com/hoberdanAC/raizes-do-nordeste-api
- Swagger (local): http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Coleção Postman: [`postman/RaizesDoNordeste.postman_collection.json`](postman/RaizesDoNordeste.postman_collection.json)
- Diagramas (DER, classes, sequência, casos de uso): [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md)
- **Guia rápido de teste:** [`COMO_TESTAR.md`](COMO_TESTAR.md)

---

## 1. Requisitos

| Item | Versão |
|---|---|
| Java (JDK) | 17 ou superior |
| Maven | 3.9+ (ou use o IntelliJ/Eclipse, que já trazem Maven embutido) |
| Banco | H2 em arquivo (padrão, nada a instalar) ou PostgreSQL 14+ (opcional) |

Principais dependências: Spring Web, Spring Data JPA, Spring Security, Bean Validation, Flyway, jjwt 0.12, springdoc-openapi 2.6.

## 2. Variáveis de ambiente

Copie `.env.example` e ajuste. Todas têm valor padrão para desenvolvimento, então a API sobe sem configurar nada.

| Variável | Descrição | Padrão |
|---|---|---|
| `SERVER_PORT` | Porta HTTP | `8080` |
| `JWT_SECRET` | Segredo de assinatura do JWT (≥ 32 caracteres) | valor de dev |
| `JWT_EXPIRATION_SECONDS` | Validade do token | `3600` |
| `SPRING_PROFILES_ACTIVE` | `postgres` para usar PostgreSQL | vazio (H2) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexão PostgreSQL (perfil `postgres`) | — |

Linux/macOS: `export JWT_SECRET=...` · Windows (PowerShell): `$env:JWT_SECRET="..."`

## 3. Instalar, criar o banco e iniciar

```bash
mvn clean install -DskipTests     # baixa dependências e compila
mvn spring-boot:run               # inicia a API em http://localhost:8080
```

Na primeira execução, o **Flyway** cria as tabelas (`V1__criar_tabelas.sql`) e aplica o seed de unidades, produtos e estoque (`V2__seed_unidades_produtos_estoque.sql`). Os usuários de teste são criados pelo `DataSeeder` (as senhas passam por BCrypt). O banco H2 fica em `./data/raizesdb.mv.db`. Para **resetar tudo**, pare a API, apague a pasta `data/` e inicie de novo.

Console do banco (dev): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/raizesdb`, usuário `sa`, senha vazia.

Com PostgreSQL:
```bash
export SPRING_PROFILES_ACTIVE=postgres DB_URL=jdbc:postgresql://localhost:5432/raizes DB_USER=raizes DB_PASSWORD=raizes
mvn spring-boot:run
```

### Usuários do seed (apenas para teste)

| Perfil | E-mail | Senha | Unidade |
|---|---|---|---|
| ADMIN | admin@raizes.com | Admin@123 | rede toda |
| GERENTE | gerente@raizes.com | Gerente@123 | 1 – Recife |
| ATENDENTE | atendente@raizes.com | Atendente@123 | 1 – Recife |
| COZINHA | cozinha@raizes.com | Cozinha@123 | 1 – Recife |
| COZINHA | cozinha.salvador@raizes.com | Cozinha@123 | 2 – Salvador |
| CLIENTE (com consentimento fidelidade) | cliente@raizes.com | Cliente@123 | — |
| CLIENTE (sem consentimento) | joao@raizes.com | Cliente@123 | — |

Seed de estoque relevante para testes: na unidade 1, o produto 3 (Baião de dois) tem só **2 unidades** (usado no teste de 409).

## 4. Como rodar os testes

**Automatizados (JUnit + MockMvc, banco H2 em memória):**
```bash
mvn test
```
Arquivo: `src/test/java/br/com/raizesdonordeste/FluxoPedidoIntegrationTest.java` (13 cenários).

**Postman:** importe `postman/RaizesDoNordeste.postman_collection.json`, confirme a variável `baseUrl` (`http://localhost:8080`) e use **Run collection** executando as pastas **na ordem** (01 → 08). Os logins salvam os tokens e o pedido criado salva o `pedidoId` automaticamente nas variáveis da coleção. Cada requisição tem asserções (`pm.test`).

**Swagger:** faça login em `POST /auth/login`, copie o `accessToken`, clique em **Authorize** e cole o token.

---

## 5. Arquitetura (camadas)

```
src/main/java/br/com/raizesdonordeste/
├── domain/            Entidades JPA e regras puras (Pedido.alterarStatus, Estoque.baixar, StatusPedido)
│   ├── enums/         Perfil, CanalPedido, StatusPedido, MetodoPagamento...
│   ├── model/         Usuario, Unidade, Produto, Estoque, Pedido, ItemPedido, Pagamento...
│   └── exception/     Exceções de negócio com código estável (sem conhecer HTTP)
├── application/       Casos de uso (orquestração do fluxo)
│   ├── service/       PedidoService, PagamentoService, FidelidadeService, EstoqueService...
│   ├── port/          Interfaces para o mundo externo: GatewayPagamento, TokenService
│   └── dto/           Contratos de request/response (records + Bean Validation)
├── infrastructure/    Detalhes técnicos
│   ├── repository/    Spring Data JPA
│   ├── security/      JWT, filtro, SecurityConfig, respostas 401/403
│   ├── payment/       GatewayPagamentoMock (implementa a porta GatewayPagamento)
│   ├── seed/          DataSeeder (usuários de teste)
│   └── config/        OpenAPI/Swagger
└── api/               Interface HTTP
    ├── controller/    Rotas REST (só traduzem HTTP <-> caso de uso)
    └── error/         ErroPadrao + GlobalExceptionHandler
```

O gateway de pagamento e o emissor de token são **portas** (interfaces) na camada de aplicação. Trocar o mock por um provedor real exige só uma nova implementação na infraestrutura.

## 6. Endpoints

Autenticação: `Authorization: Bearer <token>`. Paginação: `?page=1&limit=10` (máx. 100).

| Recurso | Método e rota | Acesso | Status principais |
|---|---|---|---|
| Início | `GET /` | público | 200 |
| Auth | `POST /auth/login` | público | 200, 401, 422 |
| Usuários | `POST /usuarios` | público (cria CLIENTE) | 201, 409, 422 |
| | `POST /usuarios/funcionarios` | ADMIN | 201, 403, 409, 422 |
| | `GET /usuarios/me` | autenticado | 200, 401 |
| | `DELETE /usuarios/me` (anonimização LGPD) | CLIENTE | 204 |
| Unidades | `GET /unidades` | público | 200 |
| | `GET /unidades/{id}/cardapio` | público | 200, 404 |
| | `POST /unidades` | ADMIN | 201, 403, 422 |
| Produtos | `GET /produtos`, `GET /produtos/{id}` | público | 200, 404 |
| | `POST /produtos` | ADMIN, GERENTE | 201, 403, 422 |
| Estoque | `GET /estoque?unidadeId=1` | ADMIN, GERENTE, ATENDENTE, COZINHA (própria unidade) | 200, 403, 404 |
| | `POST /estoque/movimentacoes` | ADMIN, GERENTE | 201, 403, 404, 409, 422 |
| Pedidos | `POST /pedidos` (header opcional `Idempotency-Key`) | CLIENTE, ATENDENTE | 201, 200 (replay), 400, 401, 403, 404, 409, 422 |
| | `GET /pedidos?canalPedido=&status=&unidadeId=` | autenticado (cliente vê os seus; funcionário, a sua unidade) | 200, 400, 401 |
| | `GET /pedidos/{id}` | dono / unidade / ADMIN | 200, 403, 404 |
| | `PATCH /pedidos/{id}/status` | COZINHA, ATENDENTE, GERENTE, ADMIN | 200, 403, 409, 422 |
| | `POST /pedidos/{id}/cancelamento` | CLIENTE (não pago), ATENDENTE, GERENTE, ADMIN | 200, 403, 409 |
| Pagamentos | `POST /pagamentos` | CLIENTE, ATENDENTE | 201, 402, 403, 404, 409, 503 |
| | `GET /pagamentos?pedidoId=1` | dono / unidade / ADMIN | 200 |
| Fidelidade | `PUT /fidelidade/consentimento` | CLIENTE | 200 |
| | `GET /fidelidade/saldo`, `GET /fidelidade/historico` | CLIENTE | 200 |
| | `POST /fidelidade/resgates` | CLIENTE | 201, 409, 422 |
| Auditoria | `GET /auditoria?acao=PEDIDO_CRIADO` | ADMIN | 200, 403 |

Detalhes de request/response e exemplos estão no Swagger.

### Exemplo: criar pedido

```http
POST /pedidos
Authorization: Bearer <token-cliente>
Idempotency-Key: 7f1c2a90-app-001
```
```json
{ "canalPedido": "TOTEM", "unidadeId": 1, "itens": [ { "produtoId": 1, "quantidade": 2 } ] }
```
Resposta 201:
```json
{
  "pedidoId": 1, "canalPedido": "TOTEM", "status": "AGUARDANDO_PAGAMENTO", "unidadeId": 1, "clienteId": 6,
  "total": 37.80,
  "itens": [ { "produtoId": 1, "nome": "Tapioca de carne de sol", "quantidade": 2, "precoUnitario": 18.90, "subtotal": 37.80 } ],
  "criadoEm": "2026-11-10T12:00:00", "atualizadoEm": "2026-11-10T12:00:00"
}
```

### Padrão de erro (todas as falhas)

```json
{
  "error": "ESTOQUE_INSUFICIENTE",
  "message": "Não há quantidade suficiente para um ou mais itens.",
  "details": [ { "field": "itens[0].quantidade", "issue": "Baião de dois - solicitado: 50, disponível: 2" } ],
  "timestamp": "2026-11-10T15:00:00Z",
  "path": "/pedidos",
  "requestId": "b3c1e0d2-..."
}
```

## 7. Regras de negócio do fluxo crítico

1. **Multicanalidade:** `canalPedido` é ENUM obrigatório. Ausente → 422; valor fora da lista → 400. CLIENTE usa APP/TOTEM/WEB/PICKUP; ATENDENTE registra BALCAO na própria unidade.
2. **Preço vem do catálogo**, nunca do cliente (evita fraude de valor).
3. **Estoque por unidade:** produto precisa estar no cardápio da unidade (404) e ter saldo (409). O estoque é **reservado na criação** (com lock pessimista contra venda simultânea) e devolvido no cancelamento.
4. **Máquina de status:** `AGUARDANDO_PAGAMENTO → PAGO → EM_PREPARACAO → PRONTO → ENTREGUE`; cancelamento só antes do preparo. Transição inválida → 409.
5. **Pagamento mock** (`GatewayPagamentoMock`): `tokenPagamento = "tok_recusado"` → 402 e pedido continua aguardando; `"tok_falha"` → 3 tentativas com espera e depois 503; valor > R$ 2.000 → recusado; demais → aprovado. Toda tentativa é gravada em `pagamentos`. Pedido já pago → 409 (não cobra duas vezes).
6. **Idempotência na criação:** header `Idempotency-Key` repetido devolve o mesmo pedido (200) em vez de duplicar.
7. **Fidelidade:** só com consentimento. 1 ponto por R$ 1 pago; resgate em múltiplos de 100 (100 pontos = R$ 5 em cupom). Cancelamento de pedido pago estorna os pontos.
8. **Promoções/campanhas (proposta, não implementado):** tabela `promocoes` (unidade opcional, produto/categoria, tipo PERCENTUAL ou VALOR_FIXO, vigência, canais aplicáveis). Seria aplicada no `PedidoService.criar` após o cálculo do total, guardando o desconto em `pedidos.valor_desconto`. Ficou fora do MVP para priorizar o fluxo crítico completo e testável.

## 8. LGPD e segurança

| Dado pessoal | Finalidade | Base legal (LGPD art. 7º) |
|---|---|---|
| Nome, e-mail | Identificação e login | Execução de contrato (V) |
| Senha (somente hash BCrypt) | Autenticação | Execução de contrato (V) |
| Histórico de pedidos | Entrega do serviço e obrigação fiscal | Contrato (V) e obrigação legal (II) |
| Pontos de fidelidade | Programa de benefícios | **Consentimento (I)**, registrado com data em `usuarios.data_consentimento` |

Controles aplicados: **minimização** (CPF não é coletado, pois nenhuma funcionalidade precisa dele; JWT carrega só id/perfil/unidade); **hash BCrypt** com salt; **JWT** assinado com expiração; **autorização por perfil** (`@PreAuthorize`) e **por registro** (cliente só vê os próprios pedidos; funcionário, a sua unidade); respostas **nunca expõem** `senha_hash`; erros 500 não vazam stack trace; **auditoria** em `logs_auditoria` (login com sucesso/falha, cadastro, criação/cancelamento de pedido, mudança de status, pagamentos, estoque, consentimento) com e-mail **mascarado**; **direito do titular**: `GET /usuarios/me` (acesso) e `DELETE /usuarios/me` (anonimização, mantendo pedidos por obrigação fiscal); consentimento revogável em `PUT /fidelidade/consentimento`.

Retenção proposta: logs de auditoria por 12 meses; contas inativas há 5 anos anonimizadas por rotina agendada (não implementada).

## 9. Plano de testes

| ID | Cenário | Endpoint | Pré-condição | Entrada | Esperado | Evidência (coleção) |
|---|---|---|---|---|---|---|
| T01 | Login válido | POST /auth/login | seed | email/senha do cliente | 200 + accessToken | 01 - Auth / T01 |
| T02 | Senha inválida | POST /auth/login | seed | senha errada | 401 CREDENCIAIS_INVALIDAS | 01 - Auth / T02 |
| T03 | Pedido válido | POST /pedidos | token cliente | APP, unidade 1, 2 tapiocas + 1 suco | 201, AGUARDANDO_PAGAMENTO, total 46.80 | 03 - Pedidos / T03 |
| T04 | Filtro por canal | GET /pedidos?canalPedido=APP | T03 | query | 200, todos APP | 03 - Pedidos / T04 |
| T05 | Pagamento aprovado | POST /pagamentos | T03 | PIX | 201, APROVADO, pedido PAGO, pontos > 0 | 04 - Pagamento / T05 |
| T06 | Pagar de novo | POST /pagamentos | T05 | mesmo pedido | 409 PEDIDO_JA_PAGO | 04 - Pagamento / T06 |
| T07 | Pagamento recusado | POST /pagamentos | pedido novo | tok_recusado | 402 PAGAMENTO_RECUSADO; pedido segue AGUARDANDO_PAGAMENTO | 04 - Pagamento / T07 |
| T08 | Cozinha atualiza status | PATCH /pedidos/{id}/status | T05 + token cozinha | EM_PREPARACAO → PRONTO → ENTREGUE (atendente) | 200 com novo status | 05 - Status |
| T09 | Sem token | GET /pedidos | — | — | 401 NAO_AUTENTICADO | 07 - Erros / T09 |
| T10 | Perfil sem permissão | POST /pedidos | token cozinha | pedido | 403 ACESSO_NEGADO | 07 - Erros / T10 |
| T11 | Campo obrigatório ausente | POST /pedidos | token cliente | sem canalPedido | 422 VALIDACAO_FALHOU | 07 - Erros / T11 |
| T12 | Formato inválido (enum) | POST /pedidos | token cliente | canalPedido "DRONE" | 400 VALOR_INVALIDO | 07 - Erros / T12 |
| T13 | Quantidade negativa | POST /pedidos | token cliente | quantidade -1 | 422 VALIDACAO_FALHOU | 07 - Erros / T13 |
| T14 | Produto inexistente | POST /pedidos | token cliente | produtoId 999 | 404 PRODUTO_NAO_ENCONTRADO | 07 - Erros / T14 |
| T15 | Unidade inexistente | POST /pedidos | token cliente | unidadeId 999 | 404 UNIDADE_NAO_ENCONTRADA | 07 - Erros / T15 |
| T16 | Estoque insuficiente | POST /pedidos | seed (baião = 2) | 50 baiões | 409 ESTOQUE_INSUFICIENTE | 07 - Erros / T16 |
| T17 | Transição inválida | PATCH /pedidos/{id}/status | pedido não pago | PRONTO | 409 TRANSICAO_STATUS_INVALIDA | 07 - Erros / T17 |
| T18 | Canal incompatível com perfil | POST /pedidos | token cliente | BALCAO | 422 CANAL_INVALIDO_PARA_PERFIL | 07 - Erros / T18 |
| T19 | Cliente na auditoria | GET /auditoria | token cliente | — | 403 | 07 - Erros / T19 |
| T20 | Auditoria registra pedido | GET /auditoria?acao=PEDIDO_CRIADO | T03 + token admin | — | 200, contém o pedidoId | 08 - Auditoria / T20 |

Os mesmos cenários estão automatizados em `FluxoPedidoIntegrationTest` (`mvn test`).
