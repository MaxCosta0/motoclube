package br.com.max.motoclube.financeiro.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraParceladaRequest(
        @NotBlank(message = "Informe a descrição da compra.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @Size(max = 120, message = "O fornecedor deve ter no máximo 120 caracteres.")
        String fornecedor,

        @NotNull(message = "Informe a categoria.")
        Long categoriaId,

        @NotNull(message = "Informe o valor total.")
        @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O valor total deve ter no máximo 2 casas decimais.")
        BigDecimal valorTotal,

        @NotNull(message = "Informe a quantidade de parcelas.")
        @Min(value = 1, message = "A compra precisa ter ao menos 1 parcela.")
        @Max(value = 360, message = "A compra pode ter no máximo 360 parcelas.")
        Integer quantidadeParcelas,

        @NotNull(message = "Informe a data da compra.")
        LocalDate dataCompra,

        @NotNull(message = "Informe o vencimento da primeira parcela.")
        LocalDate primeiroVencimento,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observacao) {}
