package br.com.max.motoclube.financeiro.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.max.motoclube.financeiro.domain.Categoria;
import br.com.max.motoclube.financeiro.domain.Lancamento;
import br.com.max.motoclube.financeiro.domain.TipoLancamento;
import br.com.max.motoclube.financeiro.service.LancamentoService;
import br.com.max.motoclube.financeiro.web.dto.LancamentoRequest;
import br.com.max.motoclube.shared.exception.RecursoNaoEncontradoException;
import br.com.max.motoclube.shared.exception.RegraNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LancamentoController.class)
class LancamentoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LancamentoService lancamentoService;

    private static final String PAYLOAD_VALIDO =
            """
            {"categoriaId": 1, "descricao": "Vendas do bar", "valor": 450.00,
             "dataCompetencia": "2026-08-28", "dataPagamento": "2026-08-28"}
            """;

    private static Lancamento lancamentoPago() {
        Categoria bar = new Categoria("Vendas do Bar", TipoLancamento.ENTRADA);
        bar.setId(1L);
        Lancamento l = new Lancamento(
                bar, "Vendas do bar", new BigDecimal("450.00"), LocalDate.of(2026, 8, 28));
        l.setId(10L);
        l.pagar(LocalDate.of(2026, 8, 28));
        return l;
    }

    @Test
    @DisplayName("cria o lançamento e devolve 201 com Location")
    void criaLancamento() throws Exception {
        given(lancamentoService.criar(any(LancamentoRequest.class))).willReturn(lancamentoPago());

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/financeiro/lancamentos/10"))
                .andExpect(jsonPath("$.status").value("PAGO"))
                .andExpect(jsonPath("$.categoria").value("Vendas do Bar"));
    }

    @Test
    @DisplayName("valor zero é recusado com 400 e aponta o campo")
    void recusaValorZero() throws Exception {
        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"categoriaId": 1, "descricao": "x", "valor": 0,
                                 "dataCompetencia": "2026-08-28"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valor"))
                .andExpect(jsonPath("$.campos[0].mensagem").value("O valor deve ser maior que zero."));
    }

    @Test
    @DisplayName("valor negativo é recusado com 400")
    void recusaValorNegativo() throws Exception {
        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"categoriaId": 1, "descricao": "x", "valor": -10.00,
                                 "dataCompetencia": "2026-08-28"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("valor"));
    }

    @Test
    @DisplayName("payload vazio aponta todos os campos obrigatórios")
    void recusaPayloadVazio() throws Exception {
        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.length()").value(4));
    }

    @Test
    @DisplayName("categoria inexistente vira 404")
    void categoriaInexistenteRetorna404() throws Exception {
        willThrow(RecursoNaoEncontradoException.de("Categoria", 999))
                .given(lancamentoService)
                .criar(any(LancamentoRequest.class));

        mockMvc.perform(post("/api/financeiro/lancamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("violação de regra de negócio vira 422")
    void regraDeNegocioRetorna422() throws Exception {
        willThrow(new RegraNegocioException("Lançamento já está pago."))
                .given(lancamentoService)
                .pagar(any(), any());

        mockMvc.perform(post("/api/financeiro/lancamentos/10/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataPagamento\": \"2026-08-28\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value("Lançamento já está pago."));
    }

    @Test
    @DisplayName("pagar sem data é recusado")
    void pagarSemDataRetorna400() throws Exception {
        mockMvc.perform(post("/api/financeiro/lancamentos/10/pagar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos[0].campo").value("dataPagamento"));
    }

    @Test
    @DisplayName("busca por id inexistente vira 404")
    void buscarInexistenteRetorna404() throws Exception {
        given(lancamentoService.buscar(99L))
                .willThrow(RecursoNaoEncontradoException.de("Lançamento", 99));

        mockMvc.perform(get("/api/financeiro/lancamentos/99")).andExpect(status().isNotFound());
    }
}
