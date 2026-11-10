# Diagramas — Raízes do Nordeste

Diagramas em Mermaid (renderizam no GitHub). Para o PDF, exporte em https://mermaid.live.

## DER (espelha `V1__criar_tabelas.sql`)

```mermaid
erDiagram
    UNIDADES ||--o{ ESTOQUES : "possui"
    PRODUTOS ||--o{ ESTOQUES : "está em"
    ESTOQUES ||--o{ MOVIMENTACOES_ESTOQUE : "registra"
    UNIDADES ||--o{ USUARIOS : "lota funcionário"
    USUARIOS ||--o{ PEDIDOS : "cliente (0..1)"
    USUARIOS ||--o{ PEDIDOS : "criado_por"
    UNIDADES ||--o{ PEDIDOS : "recebe"
    PEDIDOS ||--|{ ITENS_PEDIDO : "contém"
    PRODUTOS ||--o{ ITENS_PEDIDO : "referenciado"
    PEDIDOS ||--o{ PAGAMENTOS : "tentativas"
    USUARIOS ||--o{ FIDELIDADE_TRANSACOES : "extrato"
    PEDIDOS |o--o{ FIDELIDADE_TRANSACOES : "origem"

    UNIDADES {
      bigint id PK
      varchar nome
      varchar cidade
      varchar uf
      boolean ativa
    }
    USUARIOS {
      bigint id PK
      varchar email UK
      varchar senha_hash
      varchar perfil "CHECK"
      bigint unidade_id FK
      boolean consentimento_fidelidade
      timestamp data_consentimento
      boolean ativo
    }
    PRODUTOS {
      bigint id PK
      varchar nome
      varchar categoria
      numeric preco "CHECK > 0"
      boolean ativo
    }
    ESTOQUES {
      bigint id PK
      bigint unidade_id FK "UK(unidade,produto)"
      bigint produto_id FK
      int quantidade "CHECK >= 0"
    }
    MOVIMENTACOES_ESTOQUE {
      bigint id PK
      bigint estoque_id FK
      varchar tipo "ENTRADA/SAIDA"
      int quantidade
      varchar motivo
      bigint usuario_id FK
    }
    PEDIDOS {
      bigint id PK
      bigint cliente_id FK "nulo no balcão"
      bigint criado_por_id FK
      bigint unidade_id FK
      varchar canal_pedido "ENUM CHECK"
      varchar status "ENUM CHECK"
      numeric valor_total
      varchar chave_idempotencia "UK(criado_por)"
    }
    ITENS_PEDIDO {
      bigint id PK
      bigint pedido_id FK
      bigint produto_id FK
      int quantidade "CHECK > 0"
      numeric preco_unitario
      numeric subtotal
    }
    PAGAMENTOS {
      bigint id PK
      bigint pedido_id FK
      varchar metodo
      varchar status
      numeric valor
      varchar codigo_transacao
    }
    FIDELIDADE_TRANSACOES {
      bigint id PK
      bigint usuario_id FK
      bigint pedido_id FK
      varchar tipo
      int pontos
    }
    LOGS_AUDITORIA {
      bigint id PK
      bigint usuario_id
      varchar acao
      varchar entidade
      bigint entidade_id
      varchar detalhes
    }
```

## Casos de uso

```mermaid
flowchart LR
    C([Cliente App/Web/Totem]) --> UC1[Cadastrar-se / Login]
    C --> UC2[Consultar cardápio da unidade]
    C --> UC3[Realizar pedido]
    C --> UC4[Solicitar pagamento]
    C --> UC5[Acompanhar status]
    C --> UC6[Fidelidade: consentir, saldo, resgatar]
    A([Atendente - Balcão]) --> UC3
    A --> UC4
    A --> UC7[Entregar pedido]
    A --> UC8[Cancelar pedido]
    K([Cozinha]) --> UC9[Atualizar status: preparo / pronto]
    G([Gerente / Admin]) --> UC10[Movimentar estoque]
    G --> UC11[Cadastrar produtos/unidades/funcionários]
    G --> UC12[Consultar auditoria]
    UC4 -. include .-> GW([Gateway de Pagamento - mock])
    UC3 -. include .-> UC13[Validar estoque]
```

## Classes de domínio

```mermaid
classDiagram
    class Pedido {
      -CanalPedido canalPedido
      -StatusPedido status
      -BigDecimal valorTotal
      +adicionarItem(Produto, int)
      +alterarStatus(StatusPedido)
      +pertenceAoCliente(Long) boolean
    }
    class ItemPedido {
      -int quantidade
      -BigDecimal precoUnitario
      -BigDecimal subtotal
    }
    class Estoque {
      -int quantidade
      +possui(int) boolean
      +baixar(int)
      +repor(int)
    }
    class StatusPedido {
      <<enumeration>>
      +proximosPermitidos() Set
      +podeIrPara(StatusPedido) boolean
    }
    class GatewayPagamento {
      <<interface>>
      +processar(SolicitacaoPagamento) RespostaGateway
    }
    class GatewayPagamentoMock
    Pedido "1" *-- "1..*" ItemPedido
    ItemPedido "*" --> "1" Produto
    Pedido "*" --> "1" Unidade
    Pedido "*" --> "0..1" Usuario : cliente
    Pedido --> StatusPedido
    Estoque "*" --> "1" Unidade
    Estoque "*" --> "1" Produto
    Pagamento "*" --> "1" Pedido
    TransacaoFidelidade "*" --> "1" Usuario
    GatewayPagamento <|.. GatewayPagamentoMock
```

## Sequência do fluxo crítico

```mermaid
sequenceDiagram
    actor Cliente
    participant API as PedidoController / PagamentoController
    participant PS as PedidoService
    participant PG as PagamentoService
    participant GW as GatewayPagamentoMock
    participant DB as Banco

    Cliente->>API: POST /pedidos (JWT, canalPedido, itens)
    API->>PS: criar()
    PS->>DB: valida unidade/produtos, lock no estoque
    alt estoque insuficiente
        PS-->>Cliente: 409 ESTOQUE_INSUFICIENTE
    else ok
        PS->>DB: grava pedido AGUARDANDO_PAGAMENTO, reserva estoque, auditoria
        PS-->>Cliente: 201 pedido
    end
    Cliente->>API: POST /pagamentos
    API->>PG: pagar()
    PG->>GW: processar(valor, método, token)
    alt aprovado
        PG->>DB: pagamento APROVADO, pedido PAGO, pontos, auditoria
        PG-->>Cliente: 201 APROVADO
    else recusado
        PG->>DB: pagamento RECUSADO, auditoria
        PG-->>Cliente: 402 PAGAMENTO_RECUSADO
    else gateway fora (3 tentativas)
        PG->>DB: pagamento ERRO
        PG-->>Cliente: 503 GATEWAY_INDISPONIVEL
    end
    Note over API,DB: Cozinha: PATCH status EM_PREPARACAO → PRONTO; Atendente: ENTREGUE
```
