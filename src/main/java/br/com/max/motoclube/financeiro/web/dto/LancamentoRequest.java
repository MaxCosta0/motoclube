package br.com.max.motoclube.financeiro.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
        @NotNull(message = "Informe a categoria.")
        Long categoriaId,

        @NotBlank(message = "Informe a descrição.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "Informe o valor.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal valor,

        @NotNull(message = "Informe a data de competência.")
        LocalDate dataCompetencia,

        LocalDate dataVencimento,

        @Schema(description = "Se vier preenchida, o lançamento já nasce PAGO nessa data. "
                + "Se vier ausente, nasce PENDENTE.")
        LocalDate dataPagamento,

        @Size(max = 120, message = "A contraparte deve ter no máximo 120 caracteres.")
        String contraparte,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String observacao) {}
