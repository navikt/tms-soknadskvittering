create table soknadsevent_historikk(
    soknadsId text references soknadskvittering(soknadsid) on delete cascade,
    event text not null,
    innhold jsonb,
    produsent jsonb not null,
    tidspunkt timestamp with time zone
);

create index soknadsevent_historikk_tidspunkt on soknadsevent_historikk(tidspunkt);

