package br.com.max.motoclube.financeiro.repository;

import br.com.max.motoclube.financeiro.domain.CompraParcelada;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraParceladaRepository extends JpaRepository<CompraParcelada, Long> {

    @EntityGraph(attributePaths = {"categoria", "parcelas", "parcelas.categoria"})
    Optional<CompraParcelada> findWithParcelasById(Long id);

    @EntityGraph(attributePaths = "categoria")
    Page<CompraParcelada> findAllBy(Pageable pageable);
}
