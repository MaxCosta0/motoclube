package br.com.max.motoclube.financeiro.repository;

import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.Regime;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros combináveis da consulta de lançamentos. Um filtro ausente devolve
 * {@link Specification#unrestricted()}, de modo que a combinação não restringe nada.
 */
public final class LancamentoSpecs {

    private LancamentoSpecs() {}

    public static Specification<Lancamento> tipo(TipoLancamento tipo) {
        return tipo == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Lancamento> status(StatusLancamento status) {
        return status == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Lancamento> categoria(Long categoriaId) {
        return categoriaId == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("categoria").get("id"), categoriaId);
    }

    public static Specification<Lancamento> compra(Long compraId) {
        return compraId == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("compra").get("id"), compraId);
    }

    public static Specification<Lancamento> descricaoContem(String termo) {
        if (termo == null || termo.isBlank()) {
            return Specification.unrestricted();
        }
        String padrao = "%" + termo.trim().toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("descricao")), padrao),
                        cb.like(cb.lower(root.get("contraparte")), padrao));
    }

    /**
     * Recorta o período pela data que o regime escolhido considera: a de pagamento
     * (caixa) ou a de competência.
     */
    public static Specification<Lancamento> periodo(Regime regime, LocalDate inicio, LocalDate fim) {
        if (inicio == null && fim == null) {
            return Specification.unrestricted();
        }
        String campo = regime == Regime.CAIXA ? "dataPagamento" : "dataCompetencia";
        return (root, query, cb) -> {
            if (inicio != null && fim != null) {
                return cb.between(root.get(campo), inicio, fim);
            }
            return inicio != null
                    ? cb.greaterThanOrEqualTo(root.get(campo), inicio)
                    : cb.lessThanOrEqualTo(root.get(campo), fim);
        };
    }
}
