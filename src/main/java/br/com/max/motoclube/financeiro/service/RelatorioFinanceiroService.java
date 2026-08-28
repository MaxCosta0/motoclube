package br.com.max.motoclube.financeiro.service;

import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.LancamentoRepository;
import br.com.max.motoclube.financeiro.repository.projection.TotalMensal;
import br.com.max.motoclube.financeiro.repository.projection.TotalPorCategoria;
import br.com.max.motoclube.financeiro.web.dto.ContasAPagarResponse;
import br.com.max.motoclube.financeiro.web.dto.FluxoCaixaResponse;
import br.com.max.motoclube.financeiro.web.dto.LinhaCategoriaResponse;
import br.com.max.motoclube.financeiro.web.dto.ResumoMensalResponse;
import br.com.max.motoclube.financeiro.web.dto.SaldoResponse;
import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Relatórios do módulo financeiro. Todos operam em regime de caixa — refletem o
 * dinheiro que de fato entrou ou saiu — exceto contas a pagar, que olha para o
 * que ainda está comprometido.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioFinanceiroService {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final LancamentoRepository lancamentoRepository;

    public RelatorioFinanceiroService(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    public SaldoResponse saldoEm(LocalDate data) {
        LocalDate referencia = data == null ? LocalDate.now() : data;
        return new SaldoResponse(referencia, saldoAcumuladoAte(referencia));
    }

    public FluxoCaixaResponse fluxoCaixa(LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);

        BigDecimal saldoInicial = saldoAcumuladoAte(inicio.minusDays(1));
        BigDecimal entradas = totalCaixa(TipoLancamento.ENTRADA, inicio, fim);
        BigDecimal saidas = totalCaixa(TipoLancamento.SAIDA, inicio, fim);
        BigDecimal resultado = entradas.subtract(saidas);

        List<TotalPorCategoria> porCategoria =
                lancamentoRepository.totaisPorCategoriaNoCaixa(inicio, fim, StatusLancamento.PAGO);

        return new FluxoCaixaResponse(
                inicio,
                fim,
                saldoInicial,
                entradas,
                saidas,
                resultado,
                saldoInicial.add(resultado),
                linhas(porCategoria, TipoLancamento.ENTRADA, entradas),
                linhas(porCategoria, TipoLancamento.SAIDA, saidas));
    }

    public ResumoMensalResponse resumoMensal(int ano) {
        List<TotalMensal> totais = lancamentoRepository.resumoMensalCaixa(ano, StatusLancamento.PAGO);

        List<ResumoMensalResponse.MesResponse> meses = new ArrayList<>(12);
        BigDecimal somaEntradas = BigDecimal.ZERO;
        BigDecimal somaSaidas = BigDecimal.ZERO;

        for (int mes = 1; mes <= 12; mes++) {
            BigDecimal entradas = totalDoMes(totais, mes, TipoLancamento.ENTRADA);
            BigDecimal saidas = totalDoMes(totais, mes, TipoLancamento.SAIDA);
            somaEntradas = somaEntradas.add(entradas);
            somaSaidas = somaSaidas.add(saidas);

            meses.add(new ResumoMensalResponse.MesResponse(
                    mes,
                    Month.of(mes).getDisplayName(TextStyle.FULL, PT_BR),
                    entradas,
                    saidas,
                    entradas.subtract(saidas)));
        }

        return new ResumoMensalResponse(
                ano, meses, somaEntradas, somaSaidas, somaEntradas.subtract(somaSaidas));
    }

    public ContasAPagarResponse contasAPagar(LocalDate ate) {
        LocalDate limite = ate == null ? LocalDate.now().plusMonths(1) : ate;
        LocalDate hoje = LocalDate.now();

        List<Lancamento> pendentes = lancamentoRepository.contasAPagarAte(
                limite, TipoLancamento.SAIDA, StatusLancamento.PENDENTE);

        List<ContasAPagarResponse.ContaResponse> contas = new ArrayList<>(pendentes.size());
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;

        for (Lancamento l : pendentes) {
            boolean estaVencida = l.getDataVencimento().isBefore(hoje);
            total = total.add(l.getValor());
            if (estaVencida) {
                vencido = vencido.add(l.getValor());
            }
            contas.add(new ContasAPagarResponse.ContaResponse(
                    l.getId(),
                    l.getDescricao(),
                    l.getCategoria().getNome(),
                    l.getContraparte(),
                    l.getValor(),
                    l.getDataVencimento(),
                    ChronoUnit.DAYS.between(hoje, l.getDataVencimento()),
                    estaVencida,
                    l.getNumeroParcela(),
                    l.getTotalParcelas()));
        }

        return new ContasAPagarResponse(limite, escala(total), escala(vencido), contas);
    }

    private BigDecimal saldoAcumuladoAte(LocalDate data) {
        return escala(lancamentoRepository.saldoAcumuladoAte(
                data, TipoLancamento.ENTRADA, StatusLancamento.PAGO));
    }

    private BigDecimal totalCaixa(TipoLancamento tipo, LocalDate inicio, LocalDate fim) {
        return escala(
                lancamentoRepository.totalCaixaPorTipo(tipo, inicio, fim, StatusLancamento.PAGO));
    }

    private static BigDecimal totalDoMes(List<TotalMensal> totais, int mes, TipoLancamento tipo) {
        return totais.stream()
                .filter(t -> t.mes() == mes && t.tipo() == tipo)
                .map(TotalMensal::total)
                .findFirst()
                .map(RelatorioFinanceiroService::escala)
                .orElse(zero());
    }

    private static List<LinhaCategoriaResponse> linhas(
            List<TotalPorCategoria> totais, TipoLancamento tipo, BigDecimal totalDoTipo) {
        return totais.stream()
                .filter(t -> t.tipo() == tipo)
                .map(t -> new LinhaCategoriaResponse(
                        t.categoriaId(),
                        t.categoria(),
                        escala(t.total()),
                        t.quantidade(),
                        percentual(t.total(), totalDoTipo)))
                .toList();
    }

    private static BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return zero();
        }
        return parte.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(BigDecimal valor) {
        return valor == null ? zero() : valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe o início e o fim do período.");
        }
        if (fim.isBefore(inicio)) {
            throw new RegraNegocioException("A data final não pode ser anterior à inicial.");
        }
    }
}
