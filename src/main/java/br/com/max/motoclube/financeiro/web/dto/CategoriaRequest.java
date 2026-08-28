package br.com.max.motoclube.financeiro.web.dto;

import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "Informe o nome da categoria.")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
        String nome,

        @NotNull(message = "Informe se a categoria é de ENTRADA ou SAIDA.")
        TipoLancamento tipo,

        Boolean ativa) {}
