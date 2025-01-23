create table soknad(
    soknadsId text primary key,
    ident text not null,
    tittel text not null,
    tema text not null,
    skjemanummer text not null,
    registrert timestamp with time zone not null,
    fristEttersending date not null,
    lenkeSoknad text,
    journalpostId text,
    vedlegg jsonb,
    historikk jsonb,
    opprettet timestamp with time zone not null,
    ferdigstilt timestamp with time zone
);

create index soknad_ident on soknad(ident);
create index soknad_opprettet on soknad(opprettet);
