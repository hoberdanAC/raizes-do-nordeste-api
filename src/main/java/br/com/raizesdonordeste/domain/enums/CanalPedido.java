package br.com.raizesdonordeste.domain.enums;

/**
 * Canal de origem do pedido (multicanalidade como dado de domínio).
 * É um ENUM (e não texto livre): valores fora da lista geram erro 400.
 */
public enum CanalPedido {
    APP, TOTEM, BALCAO, PICKUP, WEB
}
