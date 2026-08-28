package br.com.max.motoclube.financeiro.web.dto;

import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoResponse(
        Long id,
        Long categoriaId,
        String categoria,
        TipoLancamento tipo,
        StatusLancamento status,
        String descricao,
        BigDecimal valor,
        LocalDate dataCompetencia,
        LocalDate dataVencimento,
        LocalDate dataPagamento,
        String contraparte,
        String observacao,
        @Schema(description = "Id da compra parcelada, se este lançamento for uma parcela")
        Long compraId,
        @Schema(description = "Posição desta parcela (1-based), se for uma parcela")
        Integer numeroParcela,
        @Schema(description = "Quantidade total de parcelas da compra, se for uma parcela")
        Integer totalParcelas) {

    public static LancamentoResponse de(Lancamento l) {
        return new LancamentoResponse(
                l.getId(),
                l.getCategoria().getId(),
                l.getCategoria().getNome(),
                l.getTipo(),
                l.getStatus(),
                l.getDescricao(),
                l.getValor(),
                l.getDataCompetencia(),
                l.getDataVencimento(),
                l.getDataPagamento(),
                l.getContraparte(),
                l.getObservacao(),
                l.getCompra() == null ? null : l.getCompra().getId(),
                l.getNumeroParcela(),
                l.getTotalParcelas());
    }
}
