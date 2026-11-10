package br.com.raizesdonordeste.application.dto;

import org.springframework.data.domain.Page;
import java.util.List;
import java.util.function.Function;

/** Envelope padrão das listagens paginadas (?page=1&limit=10). Página começa em 1. */
public record Pagina<T>(List<T> itens, int page, int limit, long totalItens, int totalPaginas) {

    public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new Pagina<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber() + 1,
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
