package br.com.max.motoclube.financeiro.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LancamentoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 28);

    private Lancamento saidaPendente() {
        Categoria aluguel = new Categoria("Aluguel", TipoLancamento.SAIDA);
        return new Lancamento(aluguel, "Aluguel da sede", new BigDecimal("800.00"), HOJE);
    }

    @Nested
    class Criacao {

        @Test
        @DisplayName("nasce pendente e herda o tipo da categoria")
        void nasceComTipoDaCategoria() {
            Lancamento lancamento = saidaPendente();

            assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PENDENTE);
            assertThat(lancamento.getTipo()).isEqualTo(TipoLancamento.SAIDA);
            assertThat(lancamento.getDataPagamento()).isNull();
        }

        @Test
        @DisplayName("trocar a categoria sincroniza o tipo")
        void trocarCategoriaSincronizaTipo() {
            Lancamento lancamento = saidaPendente();

            lancamento.trocarCategoria(new Categoria("Rifas", TipoLancamento.ENTRADA));

            assertThat(lancamento.getTipo()).isEqualTo(TipoLancamento.ENTRADA);
        }
    }

    @Nested
    class Pagamento {

        @Test
        void pagarRegistraDataEStatus() {
            Lancamento lancamento = saidaPendente();

            lancamento.pagar(HOJE);

            assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PAGO);
            assertThat(lancamento.getDataPagamento()).isEqualTo(HOJE);
        }

        @Test
        void naoPagaDuasVezes() {
            Lancamento lancamento = saidaPendente();
            lancamento.pagar(HOJE);

            assertThatThrownBy(() -> lancamento.pagar(HOJE))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já está pago");
        }

        @Test
        void naoPagaLancamentoCancelado() {
            Lancamento lancamento = saidaPendente();
            lancamento.cancelar();

            assertThatThrownBy(() -> lancamento.pagar(HOJE))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("cancelado");
        }
    }

    @Nested
    class Estorno {

        @Test
        void estornarVoltaParaPendenteELimpaAData() {
            Lancamento lancamento = saidaPendente();
            lancamento.pagar(HOJE);

            lancamento.estornar();

            assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.PENDENTE);
            assertThat(lancamento.getDataPagamento()).isNull();
        }

        @Test
        void naoEstornaOQueNaoFoiPago() {
            Lancamento lancamento = saidaPendente();

            assertThatThrownBy(lancamento::estornar)
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("estornar");
        }
    }

    @Nested
    class Cancelamento {

        @Test
        void cancelaPendente() {
            Lancamento lancamento = saidaPendente();

            lancamento.cancelar();

            assertThat(lancamento.getStatus()).isEqualTo(StatusLancamento.CANCELADO);
        }

        @Test
        @DisplayName("lançamento pago precisa ser estornado antes de cancelar")
        void naoCancelaPagoDiretamente() {
            Lancamento lancamento = saidaPendente();
            lancamento.pagar(HOJE);

            assertThatThrownBy(lancamento::cancelar)
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("Estorne o pagamento");
        }

        @Test
        void naoCancelaDuasVezes() {
            Lancamento lancamento = saidaPendente();
            lancamento.cancelar();

            assertThatThrownBy(lancamento::cancelar)
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessageContaining("já está cancelado");
        }
    }
}
