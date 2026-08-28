package br.com.max.motoclube.financeiro.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.projection.TotalMensal;
import br.com.max.motoclube.financeiro.repository.projection.TotalPorCategoria;
import br.com.max.motoclube.support.PostgresTestContainer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Exercita as agregações dos relatórios contra um Postgres real, incluindo as
 * migrations do Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestContainer.class)
class LancamentoRepositoryTest {

    private static final LocalDate JANEIRO = LocalDate.of(2026, 1, 15);
    private static final LocalDate FEVEREIRO = LocalDate.of(2026, 2, 10);

    @Autowired private LancamentoRepository lancamentoRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private Categoria bar;
    private Categoria rifas;
    private Categoria aluguel;

    @BeforeEach
    void setUp() {
        lancamentoRepository.deleteAll();
        // As categorias vêm do seed da migration V2.
        bar = categoriaRepository.findByNomeIgnoreCase("Vendas do Bar").orElseThrow();
        rifas = categoriaRepository.findByNomeIgnoreCase("Rifas").orElseThrow();
        aluguel = categoriaRepository.findByNomeIgnoreCase("Aluguel").orElseThrow();
    }

    private Lancamento pago(Categoria categoria, String valor, LocalDate dataPagamento) {
        Lancamento l = new Lancamento(categoria, "teste", new BigDecimal(valor), dataPagamento);
        l.pagar(dataPagamento);
        return lancamentoRepository.save(l);
    }

    private Lancamento pendente(Categoria categoria, String valor, LocalDate vencimento) {
        Lancamento l = new Lancamento(categoria, "teste", new BigDecimal(valor), vencimento);
        l.setDataVencimento(vencimento);
        return lancamentoRepository.save(l);
    }

    @Test
    @DisplayName("o seed da migration cria as categorias do clube")
    void seedDeCategoriasAplicado() {
        assertThat(categoriaRepository.findAll()).hasSize(10);
        assertThat(categoriaRepository.findByTipoOrderByNomeAsc(TipoLancamento.ENTRADA))
                .extracting(Categoria::getNome)
                .containsExactly("Colaborativo", "Rifas", "Vendas do Bar");
    }

    @Test
    @DisplayName("saldo acumulado soma entradas e subtrai saídas pagas")
    void saldoAcumuladoConsideraApenasPagos() {
        pago(bar, "450.00", JANEIRO);
        pago(aluguel, "800.00", JANEIRO);
        pendente(aluguel, "999.00", FEVEREIRO);

        BigDecimal saldo = lancamentoRepository.saldoAcumuladoAte(
                FEVEREIRO, TipoLancamento.ENTRADA, StatusLancamento.PAGO);

        assertThat(saldo).isEqualByComparingTo("-350.00");
    }

    @Test
    @DisplayName("saldo acumulado respeita a data de corte")
    void saldoAcumuladoRespeitaCorte() {
        pago(bar, "100.00", JANEIRO);
        pago(bar, "200.00", FEVEREIRO);

        assertThat(lancamentoRepository.saldoAcumuladoAte(
                        JANEIRO, TipoLancamento.ENTRADA, StatusLancamento.PAGO))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("lançamento cancelado não entra em nenhum total")
    void canceladoNaoEntraNoSaldo() {
        Lancamento cancelado = new Lancamento(bar, "teste", new BigDecimal("500.00"), JANEIRO);
        cancelado.cancelar();
        lancamentoRepository.save(cancelado);
        pago(bar, "100.00", JANEIRO);

        assertThat(lancamentoRepository.saldoAcumuladoAte(
                        FEVEREIRO, TipoLancamento.ENTRADA, StatusLancamento.PAGO))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("total por tipo recorta o período pela data de pagamento")
    void totalCaixaPorTipoNoPeriodo() {
        pago(bar, "450.00", JANEIRO);
        pago(rifas, "150.00", JANEIRO);
        pago(bar, "999.00", FEVEREIRO);

        BigDecimal entradasJaneiro = lancamentoRepository.totalCaixaPorTipo(
                TipoLancamento.ENTRADA,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                StatusLancamento.PAGO);

        assertThat(entradasJaneiro).isEqualByComparingTo("600.00");
    }

    @Test
    @DisplayName("quebra por categoria agrupa valor e quantidade")
    void totaisPorCategoria() {
        pago(bar, "450.00", JANEIRO);
        pago(bar, "50.00", JANEIRO);
        pago(rifas, "150.00", JANEIRO);
        pago(aluguel, "800.00", JANEIRO);

        List<TotalPorCategoria> totais = lancamentoRepository.totaisPorCategoriaNoCaixa(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), StatusLancamento.PAGO);

        assertThat(totais)
                .extracting(TotalPorCategoria::categoria, t -> t.total().stripTrailingZeros(), TotalPorCategoria::quantidade)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Vendas do Bar", new BigDecimal("500.00").stripTrailingZeros(), 2L),
                        org.assertj.core.groups.Tuple.tuple("Rifas", new BigDecimal("150.00").stripTrailingZeros(), 1L),
                        org.assertj.core.groups.Tuple.tuple("Aluguel", new BigDecimal("800.00").stripTrailingZeros(), 1L));
    }

    @Test
    @DisplayName("resumo mensal agrupa por mês e tipo dentro do ano")
    void resumoMensal() {
        pago(bar, "450.00", JANEIRO);
        pago(aluguel, "800.00", JANEIRO);
        pago(bar, "300.00", FEVEREIRO);
        pago(bar, "77.00", LocalDate.of(2025, 1, 5));

        List<TotalMensal> totais = lancamentoRepository.resumoMensalCaixa(2026, StatusLancamento.PAGO);

        assertThat(totais).hasSize(3);
        assertThat(totais)
                .filteredOn(t -> t.mes() == 1 && t.tipo() == TipoLancamento.ENTRADA)
                .singleElement()
                .extracting(TotalMensal::total, org.assertj.core.api.InstanceOfAssertFactories.BIG_DECIMAL)
                .isEqualByComparingTo("450.00");
        assertThat(totais).noneMatch(t -> t.mes() == 3);
    }

    @Test
    @DisplayName("contas a pagar traz apenas saídas pendentes até o limite, por vencimento")
    void contasAPagar() {
        pendente(aluguel, "800.00", LocalDate.of(2026, 2, 10));
        pendente(aluguel, "800.00", LocalDate.of(2026, 1, 10));
        pendente(aluguel, "800.00", LocalDate.of(2026, 12, 10));
        pago(aluguel, "800.00", JANEIRO);
        pendente(bar, "10.00", LocalDate.of(2026, 1, 20));

        List<Lancamento> contas = lancamentoRepository.contasAPagarAte(
                LocalDate.of(2026, 6, 30), TipoLancamento.SAIDA, StatusLancamento.PENDENTE);

        assertThat(contas)
                .extracting(Lancamento::getDataVencimento)
                .containsExactly(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 2, 10));
    }
}
