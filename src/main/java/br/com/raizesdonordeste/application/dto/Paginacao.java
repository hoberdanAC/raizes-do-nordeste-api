package br.com.raizesdonordeste.application.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Converte page/limit da API em PageRequest, com limites seguros (evita "limit=100000"). */
public final class Paginacao {

    public static final int LIMITE_MAXIMO = 100;

    private Paginacao() { }

    public static PageRequest de(Integer page, Integer limit, Sort sort) {
        int p = (page == null || page < 1) ? 1 : page;
        int l = (limit == null || limit < 1) ? 10 : Math.min(limit, LIMITE_MAXIMO);
        return PageRequest.of(p - 1, l, sort);
    }
}
