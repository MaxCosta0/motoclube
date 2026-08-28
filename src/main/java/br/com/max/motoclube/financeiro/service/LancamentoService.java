package br.com.max.motoclube.financeiro.service;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.Regime;
import br.com.max.motoclube.financeiro.domain.StatusLancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.LancamentoRepository;
import br.com.max.motoclube.financeiro.repository.LancamentoSpecs;
import br.com.max.motoclube.financeiro.web.dto.LancamentoRequest;
import br.com.max.motoclube.shared.exception.RecursoNaoEncontradoException;
import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final CategoriaService categoriaService;

    public LancamentoService(
            LancamentoRepository lancamentoRepository, CategoriaService categoriaService) {
        this.lancamentoRepository = lancamentoRepository;
        this.categoriaService = categoriaService;
    }

    @Transactional(readOnly = true)
    public Page<Lancamento> listar(Filtro filtro, Pageable pageable) {
        Specification<Lancamento> spec = Specification.allOf(
                LancamentoSpecs.periodo(filtro.regimeOuPadrao(), filtro.inicio(), filtro.fim()),
                LancamentoSpecs.tipo(filtro.tipo()),
                LancamentoSpecs.status(filtro.status()),
                LancamentoSpecs.categoria(filtro.categoriaId()),
                LancamentoSpecs.compra(filtro.compraId()),
                LancamentoSpecs.descricaoContem(filtro.busca()));
        return lancamentoRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Lancamento buscar(Long id) {
        return lancamentoRepository
                .findWithCategoriaById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Lançamento", id));
    }

    @Transactional
    public Lancamento criar(LancamentoRequest request) {
        Categoria categoria = categoriaService.buscarAtiva(request.categoriaId());

        Lancamento lancamento = new Lancamento(
                categoria, request.descricao().trim(), request.valor(), request.dataCompetencia());
        lancamento.setDataVencimento(request.dataVencimento());
        lancamento.setContraparte(request.contraparte());
        lancamento.setObservacao(request.observacao());

        // Sem data de pagamento o lançamento nasce pendente; com ela, já nasce pago.
        if (request.dataPagamento() != null) {
            lancamento.pagar(request.dataPagamento());
        }

        return lancamentoRepository.save(lancamento);
    }

    @Transactional
    public Lancamento atualizar(Long id, LancamentoRequest request) {
        Lancamento lancamento = buscar(id);

        if (lancamento.getStatus() == StatusLancamento.CANCELADO) {
            throw new RegraNegocioException("Lançamento cancelado não pode ser editado.");
        }
        if (lancamento.isParcela()) {
            throw new RegraNegocioException(
                    "Parcela de compra parcelada não pode ser editada isoladamente.");
        }

        if (!lancamento.getCategoria().getId().equals(request.categoriaId())) {
            lancamento.trocarCategoria(categoriaService.buscarAtiva(request.categoriaId()));
        }
        lancamento.setDescricao(request.descricao().trim());
        lancamento.setValor(request.valor());
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDataVencimento(request.dataVencimento());
        lancamento.setContraparte(request.contraparte());
        lancamento.setObservacao(request.observacao());

        // A data de pagamento tem endpoints próprios (pagar/estornar). Aqui ela só
        // é ajustada quando o lançamento já está pago.
        if (lancamento.getStatus() == StatusLancamento.PAGO) {
            if (request.dataPagamento() == null) {
                lancamento.estornar();
            } else {
                lancamento.setDataPagamento(request.dataPagamento());
            }
        } else if (request.dataPagamento() != null) {
            lancamento.pagar(request.dataPagamento());
        }

        return lancamento;
    }

    @Transactional
    public Lancamento pagar(Long id, LocalDate dataPagamento) {
        Lancamento lancamento = buscar(id);
        lancamento.pagar(dataPagamento);
        return lancamento;
    }

    @Transactional
    public Lancamento estornar(Long id) {
        Lancamento lancamento = buscar(id);
        lancamento.estornar();
        return lancamento;
    }

    @Transactional
    public Lancamento cancelar(Long id) {
        Lancamento lancamento = buscar(id);
        lancamento.cancelar();
        return lancamento;
    }

    /** Critérios da consulta de lançamentos. Campos nulos não filtram. */
    public record Filtro(
            LocalDate inicio,
            LocalDate fim,
            Regime regime,
            TipoLancamento tipo,
            StatusLancamento status,
            Long categoriaId,
            Long compraId,
            String busca) {

        public Regime regimeOuPadrao() {
            return regime == null ? Regime.COMPETENCIA : regime;
        }
    }
}
