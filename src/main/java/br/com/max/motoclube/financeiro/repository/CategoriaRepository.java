package br.com.max.motoclube.financeiro.repository;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    List<Categoria> findAllByOrderByTipoAscNomeAsc();

    List<Categoria> findByTipoOrderByNomeAsc(TipoLancamento tipo);

    List<Categoria> findByAtivaOrderByTipoAscNomeAsc(boolean ativa);

    List<Categoria> findByTipoAndAtivaOrderByNomeAsc(TipoLancamento tipo, boolean ativa);
}
