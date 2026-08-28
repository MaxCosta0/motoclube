package br.com.max.motoclube.financeiro.repository.projection;

import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import java.math.BigDecimal;

public record TotalPorCategoria(
        Long categoriaId,
        String categoria,
        TipoLancamento tipo,
        BigDecimal total,
        long quantidade) {}
