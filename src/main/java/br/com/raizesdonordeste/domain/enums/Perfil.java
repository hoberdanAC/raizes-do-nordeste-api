package br.com.raizesdonordeste.domain.enums;

/** Perfis (roles) usados na autorização. Mapeiam os atores do diagrama de casos de uso. */
public enum Perfil {
    CLIENTE,    // App / Web / Totem
    ATENDENTE,  // Balcão
    COZINHA,    // Preparo dos pedidos
    GERENTE,    // Gestão da unidade (estoque, auditoria)
    ADMIN       // Administração da rede (matriz)
}
