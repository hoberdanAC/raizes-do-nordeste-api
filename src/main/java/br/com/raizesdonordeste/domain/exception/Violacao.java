package br.com.raizesdonordeste.domain.exception;

/** Detalhe de uma regra violada (vira o item "details" do JSON de erro padrão). */
public record Violacao(String campo, String problema) {
}
