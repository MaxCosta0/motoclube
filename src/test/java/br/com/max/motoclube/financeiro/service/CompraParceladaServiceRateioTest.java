package br.com.max.motoclube.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class CompraParceladaServiceRateioTest {

    @Test
    @DisplayName("distribui a sobra de centavos na última parcela")
    void rateiaComSobraNaUltimaParcela() {
        List<BigDecimal> parcelas = CompraParceladaService.ratear(new BigDecimal("100.00"), 3);

        assertThat(parcelas)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
    }

    @Test
    @DisplayName("divisão exata gera parcelas iguais")
    void rateiaDivisaoExata() {
        List<BigDecimal> parcelas = CompraParceladaService.ratear(new BigDecimal("1200.00"), 4);

        assertThat(parcelas).containsOnly(new BigDecimal("300.00")).hasSize(4);
    }

    @Test
    @DisplayName("parcela única recebe o valor total")
    void rateiaParcelaUnica() {
        assertThat(CompraParceladaService.ratear(new BigDecimal("59.90"), 1))
                .containsExactly(new BigDecimal("59.90"));
    }

    @ParameterizedTest(name = "R$ {0} em {1}x soma exatamente o total")
    @CsvSource({
        "100.00, 3",
        "1000.00, 3",
        "0.05, 3",
        "999.99, 7",
        "1234.56, 11",
        "50.00, 12",
        "10000.00, 36"
    })
    @DisplayName("a soma das parcelas nunca difere do valor total")
    void somaDasParcelasBateComOTotal(BigDecimal total, int quantidade) {
        List<BigDecimal> parcelas = CompraParceladaService.ratear(total, quantidade);

        assertThat(parcelas).hasSize(quantidade);
        assertThat(parcelas).allSatisfy(p -> assertThat(p.scale()).isEqualTo(2));
        assertThat(parcelas.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(total);
    }
}
