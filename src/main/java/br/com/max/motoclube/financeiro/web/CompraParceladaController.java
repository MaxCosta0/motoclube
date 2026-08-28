package br.com.max.motoclube.financeiro.web;

import br.com.max.motoclube.financeiro.service.CompraParceladaService;
import br.com.max.motoclube.financeiro.web.dto.CompraParceladaRequest;
import br.com.max.motoclube.financeiro.web.dto.CompraParceladaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/compras-parceladas")
@Tag(name = "Compras Parceladas", description = "Compras a prazo, cada uma desdobrada em N lançamentos de saída")
public class CompraParceladaController {

    private final CompraParceladaService compraService;

    public CompraParceladaController(CompraParceladaService compraService) {
        this.compraService = compraService;
    }

    @Operation(summary = "Lista compras parceladas (sem as parcelas)")
    @GetMapping
    public Page<CompraParceladaResponse> listar(
            @PageableDefault(size = 20, sort = "dataCompra", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return compraService.listar(pageable).map(compra -> CompraParceladaResponse.de(compra, false));
    }

    @Operation(summary = "Busca uma compra parcelada com todas as suas parcelas")
    @GetMapping("/{id}")
    public CompraParceladaResponse buscar(@PathVariable Long id) {
        return CompraParceladaResponse.de(compraService.buscarComParcelas(id), true);
    }

    @Operation(
            summary = "Cria uma compra parcelada",
            description = "Gera a compra e todas as N parcelas (lançamentos de saída "
                    + "pendentes, um por mês a partir do primeiro vencimento) em uma única "
                    + "transação. O rateio é exato até o centavo — a sobra da divisão vai "
                    + "para a última parcela.")
    @PostMapping
    public ResponseEntity<CompraParceladaResponse> criar(
            @Valid @RequestBody CompraParceladaRequest request) {
        CompraParceladaResponse criada = CompraParceladaResponse.de(compraService.criar(request), true);
        return ResponseEntity.created(URI.create("/api/financeiro/compras-parceladas/" + criada.id()))
                .body(criada);
    }
}
