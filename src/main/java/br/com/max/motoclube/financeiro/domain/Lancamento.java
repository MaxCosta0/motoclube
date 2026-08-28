package br.com.max.motoclube.financeiro.domain;

import br.com.max.motoclube.shared.exception.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * O fato financeiro. Entradas e saídas moram na mesma tabela — é o que permite
 * apurar o fluxo de caixa somando um lugar só. O valor é sempre positivo; quem
 * dá o sinal é o {@link TipoLancamento}.
 */
@Entity
@Table(name = "lancamento")
@Getter
@Setter
@NoArgsConstructor
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Copiado da categoria na gravação: preserva o tipo mesmo se a categoria mudar. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoLancamento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusLancamento status = StatusLancamento.PENDENTE;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    /** Texto livre: quem pagou a rifa/colaborativo, ou o fornecedor da saída. */
    @Column(length = 120)
    private String contraparte;

    @Column(length = 500)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private CompraParcelada compra;

    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Lancamento(Categoria categoria, String descricao, BigDecimal valor, LocalDate dataCompetencia) {
        this.categoria = categoria;
        this.tipo = categoria.getTipo();
        this.descricao = descricao;
        this.valor = valor;
        this.dataCompetencia = dataCompetencia;
    }

    /** Troca a categoria e sincroniza o tipo — os dois nunca podem divergir. */
    public void trocarCategoria(Categoria novaCategoria) {
        this.categoria = novaCategoria;
        this.tipo = novaCategoria.getTipo();
    }

    public void pagar(LocalDate dataPagamento) {
        if (status == StatusLancamento.CANCELADO) {
            throw new RegraNegocioException("Lançamento cancelado não pode ser pago.");
        }
        if (status == StatusLancamento.PAGO) {
            throw new RegraNegocioException("Lançamento já está pago.");
        }
        this.dataPagamento = dataPagamento;
        this.status = StatusLancamento.PAGO;
    }

    /** Desfaz o pagamento, devolvendo o lançamento para pendente. */
    public void estornar() {
        if (status != StatusLancamento.PAGO) {
            throw new RegraNegocioException("Só é possível estornar um lançamento pago.");
        }
        this.dataPagamento = null;
        this.status = StatusLancamento.PENDENTE;
    }

    /**
     * Cancela um lançamento pendente. Um lançamento pago precisa ser estornado
     * antes — assim o histórico de caixa nunca muda por baixo dos panos.
     */
    public void cancelar() {
        if (status == StatusLancamento.PAGO) {
            throw new RegraNegocioException("Estorne o pagamento antes de cancelar o lançamento.");
        }
        if (status == StatusLancamento.CANCELADO) {
            throw new RegraNegocioException("Lançamento já está cancelado.");
        }
        this.status = StatusLancamento.CANCELADO;
    }

    public boolean isParcela() {
        return compra != null;
    }
}
