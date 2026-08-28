package br.com.max.motoclube.financeiro.web.dto;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;

public record CategoriaResponse(Long id, String nome, TipoLancamento tipo, boolean ativa) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(), categoria.getNome(), categoria.getTipo(), categoria.isAtiva());
    }
}
