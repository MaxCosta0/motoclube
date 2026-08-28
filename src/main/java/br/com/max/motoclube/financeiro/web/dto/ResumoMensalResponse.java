package br.com.max.motoclube.financeiro.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoMensalResponse(
        int ano,
        List<MesResponse> meses,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        BigDecimal resultado) {

    public record MesResponse(
            int mes, String nome, BigDecimal entradas, BigDecimal saidas, BigDecimal resultado) {}
}
