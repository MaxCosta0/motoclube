package br.com.max.motoclube.financeiro.service;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.CompraParcelada;
import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.CompraParceladaRepository;
import br.com.max.motoclube.financeiro.web.dto.CompraParceladaRequest;
import br.com.max.motoclube.shared.exception.RecursoNaoEncontradoException;
import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompraParceladaService {

    private final CompraParceladaRepository compraRepository;
    private final CategoriaService categoriaService;

    public CompraParceladaService(
            CompraParceladaRepository compraRepository, CategoriaService categoriaService) {
        this.compraRepository = compraRepository;
        this.categoriaService = categoriaService;
    }

    @Transactional(readOnly = true)
    public Page<CompraParcelada> listar(Pageable pageable) {
        return compraRepository.findAllBy(pageable);
    }

    @Transactional(readOnly = true)
    public CompraParcelada buscarComParcelas(Long id) {
        return compraRepository
                .findWithParcelasById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Compra parcelada", id));
    }

    @Transactional
    public CompraParcelada criar(CompraParceladaRequest request) {
        Categoria categoria = categoriaService.buscarAtiva(request.categoriaId());
        if (categoria.getTipo() != TipoLancamento.SAIDA) {
            throw new RegraNegocioException("Uma compra parcelada precisa de uma categoria de SAIDA.");
        }
        if (request.primeiroVencimento().isBefore(request.dataCompra())) {
            throw new RegraNegocioException(
                    "O vencimento da primeira parcela não pode ser anterior à data da compra.");
        }

        CompraParcelada compra = new CompraParcelada();
        compra.setDescricao(request.descricao().trim());
        compra.setFornecedor(request.fornecedor());
        compra.setCategoria(categoria);
        compra.setValorTotal(request.valorTotal());
        compra.setQuantidadeParcelas(request.quantidadeParcelas());
        compra.setDataCompra(request.dataCompra());
        compra.setPrimeiroVencimento(request.primeiroVencimento());
        compra.setObservacao(request.observacao());

        int total = request.quantidadeParcelas();
        List<BigDecimal> valores = ratear(request.valorTotal(), total);

        for (int i = 0; i < total; i++) {
            LocalDate vencimento = request.primeiroVencimento().plusMonths(i);
            String descricao = "%s (%d/%d)".formatted(compra.getDescricao(), i + 1, total);

            // A competência da parcela é o mês do vencimento: é quando a despesa
            // pesa no resultado, independente de quando for paga.
            Lancamento parcela = new Lancamento(categoria, descricao, valores.get(i), vencimento);
            parcela.setDataVencimento(vencimento);
            parcela.setContraparte(request.fornecedor());
            parcela.setNumeroParcela(i + 1);
            parcela.setTotalParcelas(total);
            compra.adicionarParcela(parcela);
        }

        return compraRepository.save(compra);
    }

    /**
     * Divide o total em N parcelas de centavos exatos. A sobra da divisão vai
     * para a última parcela, de modo que a soma das parcelas seja sempre igual
     * ao valor total — R$ 100,00 em 3x sai 33,33 / 33,33 / 33,34.
     */
    static List<BigDecimal> ratear(BigDecimal valorTotal, int quantidadeParcelas) {
        BigDecimal total = valorTotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal parcela = total.divide(BigDecimal.valueOf(quantidadeParcelas), 2, RoundingMode.DOWN);

        List<BigDecimal> valores = new ArrayList<>(quantidadeParcelas);
        for (int i = 0; i < quantidadeParcelas - 1; i++) {
            valores.add(parcela);
        }
        BigDecimal ultima = total.subtract(parcela.multiply(BigDecimal.valueOf(quantidadeParcelas - 1L)));
        valores.add(ultima);
        return valores;
    }
}
