package br.com.max.motoclube.financeiro.web;

import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.service.CategoriaService;
import br.com.max.motoclube.financeiro.web.dto.CategoriaRequest;
import br.com.max.motoclube.financeiro.web.dto.CategoriaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financeiro/categorias")
@Tag(name = "Categorias", description = "Classificam os lançamentos e fixam se eles são entrada ou saída")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Lista categorias, opcionalmente filtrando por tipo e se estão ativas")
    @GetMapping
    public List<CategoriaResponse> listar(
            @RequestParam(required = false) TipoLancamento tipo,
            @RequestParam(required = false) Boolean ativa) {
        return categoriaService.listar(tipo, ativa).stream().map(CategoriaResponse::de).toList();
    }

    @Operation(summary = "Busca uma categoria pelo id")
    @GetMapping("/{id}")
    public CategoriaResponse buscar(@PathVariable Long id) {
        return CategoriaResponse.de(categoriaService.buscar(id));
    }

    @Operation(summary = "Cria uma nova categoria")
    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse criada = CategoriaResponse.de(categoriaService.criar(request));
        return ResponseEntity.created(URI.create("/api/financeiro/categorias/" + criada.id()))
                .body(criada);
    }

    @Operation(
            summary = "Atualiza nome, tipo ou status de uma categoria",
            description = "Uma categoria que já possui lançamentos não pode trocar de tipo "
                    + "(inverteria o sinal de fatos já apurados).")
    @PutMapping("/{id}")
    public CategoriaResponse atualizar(
            @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.de(categoriaService.atualizar(id, request));
    }

    @Operation(
            summary = "Inativa a categoria",
            description = "Não apaga a categoria — ela continua existindo para o histórico "
                    + "dos lançamentos que já apontam para ela.")
    @DeleteMapping("/{id}")
    public CategoriaResponse inativar(@PathVariable Long id) {
        return CategoriaResponse.de(categoriaService.inativar(id));
    }
}
