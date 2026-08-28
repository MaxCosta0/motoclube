package br.com.max.motoclube.financeiro.web.dto;

import br.com.max.motoclube.financeiro.domain.CompraParcelada;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraParceladaResponse(
        Long id,
        String descricao,
        String fornecedor,
        Long categoriaId,
        String categoria,
        BigDecimal valorTotal,
        Integer quantidadeParcelas,
        LocalDate dataCompra,
        LocalDate primeiroVencimento,
        String observacao,
        List<LancamentoResponse> parcelas) {

    public static CompraParceladaResponse de(CompraParcelada compra, boolean incluirParcelas) {
        return new CompraParceladaResponse(
                compra.getId(),
                compra.getDescricao(),
                compra.getFornecedor(),
                compra.getCategoria().getId(),
                compra.getCategoria().getNome(),
                compra.getValorTotal(),
                compra.getQuantidadeParcelas(),
                compra.getDataCompra(),
                compra.getPrimeiroVencimento(),
                compra.getObservacao(),
                incluirParcelas
                        ? compra.getParcelas().stream().map(LancamentoResponse::de).toList()
                        : null);
    }
}
