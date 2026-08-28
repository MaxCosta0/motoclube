package br.com.max.motoclube.financeiro.web.dto;

import java.math.BigDecimal;

public record LinhaCategoriaResponse(
        Long categoriaId,
        String categoria,
        BigDecimal total,
        long quantidade,
        /** Participação da categoria no total do seu tipo, em percentual. */
        BigDecimal percentual) {}
