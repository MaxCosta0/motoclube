package br.com.max.motoclube.financeiro.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContasAPagarResponse(
        LocalDate ate,
        BigDecimal totalComprometido,
        BigDecimal totalVencido,
        List<ContaResponse> contas) {

    public record ContaResponse(
            Long lancamentoId,
            String descricao,
            String categoria,
            String contraparte,
            BigDecimal valor,
            LocalDate dataVencimento,
            /** Negativo quando já venceu. */
            long diasAteVencimento,
            boolean vencida,
            Integer numeroParcela,
            Integer totalParcelas) {}
}
