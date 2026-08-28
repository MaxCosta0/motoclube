package br.com.max.motoclube.financeiro.service;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.repository.CategoriaRepository;
import br.com.max.motoclube.financeiro.repository.LancamentoRepository;
import br.com.max.motoclube.financeiro.web.dto.CategoriaRequest;
import br.com.max.motoclube.shared.exception.RecursoNaoEncontradoException;
import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final LancamentoRepository lancamentoRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository, LancamentoRepository lancamentoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.lancamentoRepository = lancamentoRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar(TipoLancamento tipo, Boolean ativa) {
        if (tipo != null && ativa != null) {
            return categoriaRepository.findByTipoAndAtivaOrderByNomeAsc(tipo, ativa);
        }
        if (tipo != null) {
            return categoriaRepository.findByTipoOrderByNomeAsc(tipo);
        }
        if (ativa != null) {
            return categoriaRepository.findByAtivaOrderByTipoAscNomeAsc(ativa);
        }
        return categoriaRepository.findAllByOrderByTipoAscNomeAsc();
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Long id) {
        return categoriaRepository
                .findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", id));
    }

    @Transactional
    public Categoria criar(CategoriaRequest request) {
        if (categoriaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RegraNegocioException("Já existe uma categoria chamada '%s'.".formatted(request.nome()));
        }
        Categoria categoria = new Categoria(request.nome().trim(), request.tipo());
        categoria.setAtiva(request.ativa() == null || request.ativa());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscar(id);

        categoriaRepository
                .findByNomeIgnoreCase(request.nome())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new RegraNegocioException(
                            "Já existe uma categoria chamada '%s'.".formatted(request.nome()));
                });

        // Trocar o tipo inverteria o sinal de lançamentos já apurados.
        if (categoria.getTipo() != request.tipo() && lancamentoRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException(
                    "A categoria já possui lançamentos e não pode mudar de tipo. "
                            + "Inative-a e crie uma nova categoria.");
        }

        categoria.setNome(request.nome().trim());
        categoria.setTipo(request.tipo());
        if (request.ativa() != null) {
            categoria.setAtiva(request.ativa());
        }
        return categoria;
    }

    /** Categoria não é apagada — há lançamentos históricos apontando para ela. */
    @Transactional
    public Categoria inativar(Long id) {
        Categoria categoria = buscar(id);
        categoria.setAtiva(false);
        return categoria;
    }

    /** Resolve a categoria de um lançamento novo, recusando as inativas. */
    @Transactional(readOnly = true)
    public Categoria buscarAtiva(Long id) {
        Categoria categoria = buscar(id);
        if (!categoria.isAtiva()) {
            throw new RegraNegocioException(
                    "A categoria '%s' está inativa e não aceita novos lançamentos."
                            .formatted(categoria.getNome()));
        }
        return categoria;
    }
}
