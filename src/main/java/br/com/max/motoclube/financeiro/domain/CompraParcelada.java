package br.com.max.motoclube.financeiro.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agrupa uma compra a prazo. As parcelas são {@link Lancamento} de saída
 * geradas no momento da criação, cada uma com seu próprio vencimento.
 */
@Entity
@Table(name = "compra_parcelada")
@Getter
@Setter
@NoArgsConstructor
public class CompraParcelada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(length = 120)
    private String fornecedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "quantidade_parcelas", nullable = false)
    private Integer quantidadeParcelas;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @Column(name = "primeiro_vencimento", nullable = false)
    private LocalDate primeiroVencimento;

    @Column(length = 500)
    private String observacao;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroParcela asc")
    private List<Lancamento> parcelas = new ArrayList<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public void adicionarParcela(Lancamento parcela) {
        parcela.setCompra(this);
        this.parcelas.add(parcela);
    }
}
