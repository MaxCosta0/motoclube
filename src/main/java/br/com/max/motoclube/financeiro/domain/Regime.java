package br.com.max.motoclube.financeiro.domain;

/**
 * Define por qual data um periodo é filtrado.
 *
 * <p>CAIXA olha para a data em que o dinheiro efetivamente entrou ou saiu
 * ({@code dataPagamento}); COMPETENCIA olha para o mês a que o fato pertence
 * ({@code dataCompetencia}), independente de ter sido pago.
 */
public enum Regime {
    CAIXA,
    COMPETENCIA
}
