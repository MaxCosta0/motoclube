# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
docker compose up -d          # Postgres 16 (compose.yaml)
./mvnw spring-boot:run        # runs on :8080; Flyway applies migrations automatically
./mvnw test                   # requires Docker — repository tests use Testcontainers
./mvnw test -Dtest=NomeDaClasse
./mvnw compile                # compile only, no tests
docker compose down -v && docker compose up -d   # reset the local database
```

## Stack

Spring Boot 4.1.1, Java 21, Maven, PostgreSQL + Flyway + Spring Data JPA, Lombok.

Boot 4 relocated several things relative to Boot 3 — worth knowing before assuming a class lives where it used to:
- Jackson 3 (package `tools.jackson`, not `com.fasterxml`); properties like `write-dates-as-timestamps` under `SerializationFeature` no longer exist.
- Test slice annotations moved packages: `DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure`, `AutoConfigureTestDatabase` → `org.springframework.boot.jdbc.test.autoconfigure`, `WebMvcTest` → `org.springframework.boot.webmvc.test.autoconfigure`.
- The web starter is `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test`, not `-web`.
- Testcontainers is on 2.x: artifactIds renamed (`testcontainers-postgresql`, `testcontainers-junit-jupiter`) and `PostgreSQLContainer` is no longer generic.
- `Specification.allOf(...)` rejects `null` elements — build combinable filters with `Specification.unrestricted()` instead of returning `null` (see `LancamentoSpecs`).

## Architecture

### Domain model (módulo financeiro)

A single `lancamento` table represents both income and expenses — the sign comes from the `tipo` field (`ENTRADA`/`SAIDA`), never from a negative `valor`. This is what lets cash flow be computed by summing one table.

Three dates on `Lancamento` give two different views of the business:
- `dataCompetencia` — accrual basis (which month the fact belongs to)
- `dataVencimento` — due date, basis for contas a pagar
- `dataPagamento` — cash basis, the basis for the actual balance

Status (`PENDENTE`/`PAGO`/`CANCELADO`) transitions live on the entity itself — `Lancamento.pagar()`, `estornar()`, `cancelar()` — not in the service layer. Nothing is ever deleted; a paid entry must be `estornar()`-ed before it can be `cancelar()`-ed, so the cash history never changes retroactively. `CANCELADO` never counts toward any total.

`CompraParcelada` generates N `Lancamento` (SAIDA/PENDENTE) at creation time, one per month from `primeiroVencimento`. The split is exact to the cent — remainder goes to the last installment — implemented in `CompraParceladaService.ratear()`; any change to that method must keep `sum(parcelas) == valorTotal` exactly.

### Package structure

`br.com.max.motoclube.financeiro.{domain,repository,service,web,web.dto}` plus `br.com.max.motoclube.shared.exception`.

- `domain` — JPA entities; state-transition rules live here, not in services.
- `repository` — Spring Data interfaces; aggregations are `@Query` JPQL with record-based projections (`TotalPorCategoria`, `TotalMensal`); combinable list filters are `Specification`s in `LancamentoSpecs`.
- `service` — orchestration and validation; each entity mutation goes through here even though the rules themselves live in `domain`.
- `web` — thin controllers; `web/dto` holds request/response `record`s — entities are never returned directly.

`RelatorioFinanceiroService` always aggregates in SQL via projections, never in a Java loop — follow that pattern for any new report.

### Error handling

`RegraNegocioException` → 422, `RecursoNaoEncontradoException` → 404, Bean Validation failures → 400 with a `campos[]` list of field/message pairs. All mapped centrally in `ApiExceptionHandler` (`@RestControllerAdvice`) — new business rules should throw one of the two existing exceptions rather than inventing new ones.

### Schema

Flyway owns the schema (`spring.jpa.hibernate.ddl-auto=validate`). Any schema change is a new migration under `src/main/resources/db/migration/`, never a Hibernate-driven change.

## API

REST API under `/api/financeiro`. See [README.md](README.md) for the endpoint list and curl examples. The full spec is generated at runtime by springdoc-openapi from the controllers, DTOs, and Bean Validation annotations — Swagger UI at `/swagger-ui.html`, raw spec at `/v3/api-docs`. Global metadata lives in `OpenApiConfig` (`shared/config`); per-endpoint documentation is `@Operation`/`@Parameter` on the controllers and `@Schema` on the DTO fields whose name isn't self-explanatory. Don't hand-write `@ApiResponse`/`@Schema` for the 400/404/422 error shape — it's uniform across the API (see Error handling above) and documented once in `OpenApiConfig`'s `info.description`, not per operation.
