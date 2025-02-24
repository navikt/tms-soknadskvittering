create table soknadsevent_historikk(
    soknadsId text not null,
    event text not null,
    innhold jsonb,
    produsent jsonb not null,
    tidspunkt timestamp with time zone
);

create index soknadsevent_historikk_soknadsid on soknadsevent_historikk(soknadsId);
create index soknadsevent_historikk_tidspunkt on soknadsevent_historikk(tidspunkt);

