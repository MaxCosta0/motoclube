package br.com.max.motoclube.financeiro.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fluxo de caixa do período: o que entrou, o que saiu e como o saldo evoluiu.
 * Base caixa — considera apenas lançamentos efetivamente pagos.
 */
public record FluxoCaixaResponse(
        LocalDate inicio,
        LocalDate fim,
        BigDecimal saldoInicial,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        /** Entradas menos saídas no período. */
        BigDecimal resultado,
        BigDecimal saldoFinal,
        List<LinhaCategoriaResponse> entradasPorCategoria,
        List<LinhaCategoriaResponse> saidasPorCategoria) {}
