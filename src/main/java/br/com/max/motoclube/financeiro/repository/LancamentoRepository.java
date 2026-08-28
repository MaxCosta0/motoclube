package br.com.max.motoclube.financeiro.repository;

import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.projection.TotalMensal;
import br.com.max.motoclube.financeiro.repository.projection.TotalPorCategoria;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Todas as agregações são resolvidas no banco. Nada de somar em laço no Java —
 * o volume de lançamentos cresce mês a mês e os relatórios são o uso principal.
 */
public interface LancamentoRepository
        extends JpaRepository<Lancamento, Long>, JpaSpecificationExecutor<Lancamento> {

    @Override
    @EntityGraph(attributePaths = "categoria")
    Page<Lancamento> findAll(
            org.springframework.data.jpa.domain.Specification<Lancamento> spec, Pageable pageable);

    /**
     * Traz a categoria junto: com {@code open-in-view=false} o proxy lazy morre
     * antes da serialização da resposta.
     */
    @EntityGraph(attributePaths = "categoria")
    Optional<Lancamento> findWithCategoriaById(Long id);

    boolean existsByCategoriaId(Long categoriaId);

    /**
     * Saldo em caixa acumulado até a data (inclusive): entradas pagas menos
     * saídas pagas. É a base do saldo inicial dos relatórios de período.
     */
    @Query("""
            select coalesce(sum(case when l.tipo = :entrada then l.valor else -l.valor end), 0)
            from Lancamento l
            where l.status = :pago
              and l.dataPagamento <= :ate
            """)
    BigDecimal saldoAcumuladoAte(
            @Param("ate") LocalDate ate,
            @Param("entrada") TipoLancamento entrada,
            @Param("pago") StatusLancamento pago);

    /** Total movimentado por tipo no período, em regime de caixa. */
    @Query("""
            select coalesce(sum(l.valor), 0)
            from Lancamento l
            where l.status = :pago
              and l.tipo = :tipo
              and l.dataPagamento between :inicio and :fim
            """)
    BigDecimal totalCaixaPorTipo(
            @Param("tipo") TipoLancamento tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("pago") StatusLancamento pago);

    /** Quebra do período por categoria, em regime de caixa. */
    @Query("""
            select new br.com.max.motoclube.financeiro.repository.projection.TotalPorCategoria(
                c.id, c.nome, l.tipo, sum(l.valor), count(l))
            from Lancamento l
            join l.categoria c
            where l.status = :pago
              and l.dataPagamento between :inicio and :fim
            group by c.id, c.nome, l.tipo
            order by l.tipo asc, sum(l.valor) desc
            """)
    List<TotalPorCategoria> totaisPorCategoriaNoCaixa(
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("pago") StatusLancamento pago);

    /** Movimento de caixa mês a mês dentro de um ano. */
    @Query("""
            select new br.com.max.motoclube.financeiro.repository.projection.TotalMensal(
                extract(month from l.dataPagamento), l.tipo, sum(l.valor))
            from Lancamento l
            where l.status = :pago
              and extract(year from l.dataPagamento) = :ano
            group by extract(month from l.dataPagamento), l.tipo
            """)
    List<TotalMensal> resumoMensalCaixa(@Param("ano") int ano, @Param("pago") StatusLancamento pago);

    /** Saídas em aberto até a data limite, das mais antigas para as mais novas. */
    @EntityGraph(attributePaths = "categoria")
    @Query("""
            select l
            from Lancamento l
            where l.status = :pendente
              and l.tipo = :saida
              and l.dataVencimento is not null
              and l.dataVencimento <= :ate
            order by l.dataVencimento asc, l.id asc
            """)
    List<Lancamento> contasAPagarAte(
            @Param("ate") LocalDate ate,
            @Param("saida") TipoLancamento saida,
            @Param("pendente") StatusLancamento pendente);
}
