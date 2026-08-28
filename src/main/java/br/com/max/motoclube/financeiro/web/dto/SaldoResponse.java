package br.com.max.motoclube.financeiro.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaldoResponse(LocalDate data, BigDecimal saldo) {}
