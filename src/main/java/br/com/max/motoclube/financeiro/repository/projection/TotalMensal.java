package br.com.max.motoclube.financeiro.repository.projection;

import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import java.math.BigDecimal;

public record TotalMensal(int mes, TipoLancamento tipo, BigDecimal total) {}
