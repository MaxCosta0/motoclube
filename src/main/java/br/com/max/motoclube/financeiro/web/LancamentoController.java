package br.com.max.motoclube.financeiro.web;

import br.com.max.motoclube.financeiro.domain.Regime;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.service.LancamentoService;
import br.com.max.motoclube.financeiro.web.dto.LancamentoRequest;
import br.com.max.motoclube.financeiro.web.dto.LancamentoResponse;
import br.com.max.motoclube.financeiro.web.dto.PagamentoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/lancamentos")
@Tag(name = "Lançamentos", description = "O fato financeiro — entradas e saídas, na mesma tabela")
public class LancamentoController {

    private final LancamentoService lancamentoService;

    public LancamentoController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    @Operation(
            summary = "Lista lançamentos com filtros, paginado",
            description = "O período (`inicio`/`fim`) é recortado pela data que o `regime` "
                    + "escolher: `CAIXA` filtra por `dataPagamento`, `COMPETENCIA` (padrão) "
                    + "filtra por `dataCompetencia`.")
    @GetMapping
    public Page<LancamentoResponse> listar(
            @Parameter(description = "Início do período (inclusive)") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fim do período (inclusive)") @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @Parameter(description = "Qual data usar para recortar o período: CAIXA "
                            + "(dataPagamento) ou COMPETENCIA (dataCompetencia, padrão)")
                    @RequestParam(required = false) Regime regime,
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) StatusLancamento status,
            @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Filtra pelas parcelas de uma compra parcelada")
                    @RequestParam(required = false) Long compraId,
            @Parameter(description = "Busca por trecho na descrição ou na contraparte")
                    @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "dataCompetencia", direction = Sort.Direction.DESC)
                    Pageable pageable) {

        var filtro = new LancamentoService.Filtro(
                inicio, fim, regime, tipo, status, categoriaId, compraId, busca);
        return lancamentoService.listar(filtro, pageable).map(LancamentoResponse::de);
    }

    @Operation(summary = "Busca um lançamento pelo id")
    @GetMapping("/{id}")
    public LancamentoResponse buscar(@PathVariable Long id) {
        return LancamentoResponse.de(lancamentoService.buscar(id));
    }

    @Operation(
            summary = "Registra uma entrada ou saída",
            description = "Sem `dataPagamento` o lançamento nasce PENDENTE; enviando "
                    + "`dataPagamento` ele já nasce PAGO.")
    @PostMapping
    public ResponseEntity<LancamentoResponse> criar(@Valid @RequestBody LancamentoRequest request) {
        LancamentoResponse criado = LancamentoResponse.de(lancamentoService.criar(request));
        return ResponseEntity.created(URI.create("/api/financeiro/lancamentos/" + criado.id()))
                .body(criado);
    }

    @Operation(
            summary = "Edita um lançamento",
            description = "Uma parcela de compra parcelada não pode ser editada isoladamente.")
    @PutMapping("/{id}")
    public LancamentoResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody LancamentoRequest request) {
        return LancamentoResponse.de(lancamentoService.atualizar(id, request));
    }

    @Operation(
            summary = "Marca o lançamento como pago",
            description = "Falha se o lançamento já estiver pago ou cancelado.")
    @PostMapping("/{id}/pagar")
    public LancamentoResponse pagar(
            @PathVariable Long id, @Valid @RequestBody PagamentoRequest request) {
        return LancamentoResponse.de(lancamentoService.pagar(id, request.dataPagamento()));
    }

    @Operation(summary = "Desfaz o pagamento, devolvendo o lançamento para pendente")
    @PostMapping("/{id}/estornar")
    public LancamentoResponse estornar(@PathVariable Long id) {
        return LancamentoResponse.de(lancamentoService.estornar(id));
    }

    @Operation(
            summary = "Cancela um lançamento pendente",
            description = "Um lançamento pago precisa ser estornado antes de ser cancelado, "
                    + "para o histórico de caixa não mudar retroativamente.")
    @PostMapping("/{id}/cancelar")
    public LancamentoResponse cancelar(@PathVariable Long id) {
        return LancamentoResponse.de(lancamentoService.cancelar(id));
    }
}
