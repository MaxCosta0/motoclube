package br.com.max.motoclube.financeiro.web;

import br.com.max.motoclube.financeiro.service.RelatorioFinanceiroService;
import br.com.max.motoclube.financeiro.web.dto.ContasAPagarResponse;
import br.com.max.motoclube.financeiro.web.dto.FluxoCaixaResponse;
import br.com.max.motoclube.financeiro.web.dto.ResumoMensalResponse;
import br.com.max.motoclube.financeiro.web.dto.SaldoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/relatorios")
@Tag(name = "Relatórios", description = "Visão do negócio: saldo, fluxo de caixa, resumo mensal e contas a pagar")
public class RelatorioController {

    private final RelatorioFinanceiroService relatorioService;

    public RelatorioController(RelatorioFinanceiroService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @Operation(summary = "Saldo em caixa na data informada (padrão: hoje)")
    @GetMapping("/saldo")
    public SaldoResponse saldo(
            @Parameter(description = "Data de referência (padrão: hoje)")
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate data) {
        return relatorioService.saldoEm(data);
    }

    @Operation(
            summary = "Fluxo de caixa do período",
            description = "Regime de caixa: entradas, saídas, resultado e quebra por "
                    + "categoria, considerando apenas lançamentos efetivamente pagos.")
    @GetMapping("/fluxo-caixa")
    public FluxoCaixaResponse fluxoCaixa(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.fluxoCaixa(inicio, fim);
    }

    @Operation(summary = "Os 12 meses do ano em regime de caixa, para ver a tendência")
    @GetMapping("/resumo-mensal")
    public ResumoMensalResponse resumoMensal(
            @Parameter(description = "Ano de referência (padrão: ano atual)")
                    @RequestParam(required = false) Integer ano) {
        return relatorioService.resumoMensal(ano == null ? LocalDate.now().getYear() : ano);
    }

    @Operation(
            summary = "Saídas pendentes até a data informada",
            description = "Padrão: vencendo até o próximo mês. Destaca o total já vencido.")
    @GetMapping("/contas-a-pagar")
    public ContasAPagarResponse contasAPagar(
            @Parameter(description = "Data limite (padrão: hoje + 1 mês)")
                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate ate) {
        return relatorioService.contasAPagar(ate);
    }
}
