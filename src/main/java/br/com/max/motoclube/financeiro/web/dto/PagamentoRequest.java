package br.com.max.motoclube.financeiro.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PagamentoRequest(
        @NotNull(message = "Informe a data do pagamento.")
        LocalDate dataPagamento) {}
