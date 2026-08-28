# motoclube

Sistema de gestão do motoclube. O primeiro módulo é o **financeiro**.

## Módulo financeiro

Controla o fluxo de entradas e saídas do clube.

- **Entradas**: vendas do bar, colaborativo, rifas.
- **Saídas**: aluguel, água, luz, internet, reposição de estoque, reposição de materiais, compras parceladas.

### Como o dinheiro é representado

Tudo é um `lancamento` — entradas e saídas na mesma tabela, o que permite apurar o
fluxo de caixa somando um lugar só. O valor é sempre positivo; quem dá o sinal é o
`tipo` (`ENTRADA`/`SAIDA`), herdado da categoria.

Cada lançamento carrega três datas, e é a distinção entre elas que dá as duas visões
do negócio:

| Data | Para que serve |
| --- | --- |
| `dataCompetencia` | A que mês o fato pertence — **regime de competência** |
| `dataVencimento` | Quando a conta vence (base do contas a pagar) |
| `dataPagamento` | Quando o dinheiro de fato entrou ou saiu — **regime de caixa** |

Um lançamento nasce `PENDENTE` (ou já `PAGO`, se a requisição trouxer `dataPagamento`).
Ele nunca é apagado: para desfazer um pagamento use `/estornar`, e para anular um
lançamento pendente use `/cancelar`. `CANCELADO` não entra em nenhum total.

**Compras parceladas** geram N lançamentos de saída pendentes, um por mês a partir do
primeiro vencimento. A divisão é exata em centavos — R$ 1.000,00 em 3x vira
333,33 / 333,33 / 333,34, com a sobra na última parcela.

## Rodando

```bash
docker compose up -d && ./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O Flyway cria o schema e cadastra as
categorias do clube na primeira execução.

Documentação da API (OpenAPI, gerada a partir do código):
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON da especificação: `http://localhost:8080/v3/api-docs`

Testes (usam Testcontainers, então precisam do Docker):

```bash
./mvnw test
```

## API

Prefixo: `/api/financeiro`

### Lançamentos

| Método | Rota | O que faz |
| --- | --- | --- |
| `POST` | `/lancamentos` | Registra uma entrada ou saída |
| `GET` | `/lancamentos` | Lista com filtros (veja abaixo), paginado |
| `GET` | `/lancamentos/{id}` | Detalhe |
| `PUT` | `/lancamentos/{id}` | Edita |
| `POST` | `/lancamentos/{id}/pagar` | Marca como pago |
| `POST` | `/lancamentos/{id}/estornar` | Desfaz o pagamento |
| `POST` | `/lancamentos/{id}/cancelar` | Cancela um pendente |

Filtros da listagem: `inicio`, `fim`, `regime` (`CAIXA` ou `COMPETENCIA`, padrão
`COMPETENCIA`), `tipo`, `status`, `categoriaId`, `compraId`, `busca`.

### Compras parceladas

| Método | Rota | O que faz |
| --- | --- | --- |
| `POST` | `/compras-parceladas` | Cria a compra e gera todas as parcelas |
| `GET` | `/compras-parceladas` | Lista |
| `GET` | `/compras-parceladas/{id}` | Detalhe com as parcelas |

### Categorias

`GET`, `POST`, `PUT` em `/categorias`; `DELETE /categorias/{id}` **inativa** a categoria
(não apaga — há lançamentos históricos apontando para ela). Uma categoria com
lançamentos não pode trocar de tipo.

### Relatórios

| Rota | O que responde |
| --- | --- |
| `GET /relatorios/saldo?data=` | Quanto tem em caixa hoje (ou na data) |
| `GET /relatorios/fluxo-caixa?inicio=&fim=` | Entradas, saídas, resultado e quebra por categoria |
| `GET /relatorios/resumo-mensal?ano=` | Os 12 meses do ano, para ver tendência |
| `GET /relatorios/contas-a-pagar?ate=` | O que está comprometido, e o que já venceu |

### Exemplos

```bash
# Venda do bar, já recebida
curl -X POST http://localhost:8080/api/financeiro/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{"categoriaId":1,"descricao":"Vendas do bar - sabado","valor":450.00,
       "dataCompetencia":"2026-08-28","dataPagamento":"2026-08-28"}'
```

```bash
# Aluguel a vencer
curl -X POST http://localhost:8080/api/financeiro/lancamentos \
  -H 'Content-Type: application/json' \
  -d '{"categoriaId":4,"descricao":"Aluguel da sede","valor":800.00,
       "dataCompetencia":"2026-08-28","dataVencimento":"2026-09-10","contraparte":"Sr. Antonio"}'
```

```bash
# Compra parcelada em 3x
curl -X POST http://localhost:8080/api/financeiro/compras-parceladas \
  -H 'Content-Type: application/json' \
  -d '{"descricao":"Freezer para o bar","fornecedor":"Eletro Sul","categoriaId":10,
       "valorTotal":1000.00,"quantidadeParcelas":3,
       "dataCompra":"2026-08-28","primeiroVencimento":"2026-09-10"}'
```

## Erros

| HTTP | Quando |
| --- | --- |
| `400` | Payload inválido — a resposta traz `campos[]` apontando cada problema |
| `404` | Recurso inexistente |
| `422` | Regra de negócio violada (pagar duas vezes, cancelar um pago, etc.) |

## Ainda fora do escopo

Cadastro de membros (hoje o pagador é texto livre em `contraparte`), controle de
estoque do bar, múltiplas contas/caixas, autenticação e interface web.
