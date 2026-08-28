create table categoria (
    id          bigserial    primary key,
    nome        varchar(80)  not null unique,
    tipo        varchar(10)  not null,
    ativa       boolean      not null default true,
    created_at  timestamptz  not null default now(),
    updated_at  timestamptz  not null default now(),
    constraint ck_categoria_tipo check (tipo in ('ENTRADA', 'SAIDA'))
);

create table compra_parcelada (
    id                   bigserial      primary key,
    descricao            varchar(255)   not null,
    fornecedor           varchar(120),
    categoria_id         bigint         not null references categoria (id),
    valor_total          numeric(12, 2) not null,
    quantidade_parcelas  integer        not null,
    data_compra          date           not null,
    primeiro_vencimento  date           not null,
    observacao           varchar(500),
    created_at           timestamptz    not null default now(),
    updated_at           timestamptz    not null default now(),
    constraint ck_compra_valor_total check (valor_total > 0),
    constraint ck_compra_qtd_parcelas check (quantidade_parcelas between 1 and 360)
);

create table lancamento (
    id                bigserial      primary key,
    categoria_id      bigint         not null references categoria (id),
    tipo              varchar(10)    not null,
    status            varchar(10)    not null,
    descricao         varchar(255)   not null,
    valor             numeric(12, 2) not null,
    data_competencia  date           not null,
    data_vencimento   date,
    data_pagamento    date,
    contraparte       varchar(120),
    observacao        varchar(500),
    compra_id         bigint         references compra_parcelada (id),
    numero_parcela    integer,
    total_parcelas    integer,
    created_at        timestamptz    not null default now(),
    updated_at        timestamptz    not null default now(),
    constraint ck_lancamento_tipo check (tipo in ('ENTRADA', 'SAIDA')),
    constraint ck_lancamento_status check (status in ('PENDENTE', 'PAGO', 'CANCELADO')),
    constraint ck_lancamento_valor check (valor > 0),
    -- PAGO exige data de pagamento; os demais status nao podem te-la.
    constraint ck_lancamento_pagamento check (
        (status = 'PAGO' and data_pagamento is not null)
        or (status <> 'PAGO' and data_pagamento is null)
    ),
    -- Os campos de parcela andam sempre juntos.
    constraint ck_lancamento_parcela check (
        (compra_id is null and numero_parcela is null and total_parcelas is null)
        or (compra_id is not null and numero_parcela is not null and total_parcelas is not null)
    )
);

create index ix_lancamento_competencia on lancamento (data_competencia);
create index ix_lancamento_pagamento on lancamento (data_pagamento) where status = 'PAGO';
create index ix_lancamento_a_pagar on lancamento (status, data_vencimento);
create index ix_lancamento_categoria on lancamento (categoria_id);
create index ix_lancamento_compra on lancamento (compra_id);
