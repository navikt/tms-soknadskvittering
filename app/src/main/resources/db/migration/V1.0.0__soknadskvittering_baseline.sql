create table soknad(
    soknadsId text primary key,
    ident text not null,
    tittel text not null,
    temakode text not null,
    skjemanummer text not null,
    mottatt timestamp with time zone not null,
    fristEttersending date not null,
    linkSoknad text,
    journalpostId text,
    vedlegg jsonb,
    opprettet timestamp with time zone not null,
    ferdigstilt timestamp with time zone
);

create index soknad_ident on soknad(ident);
create index soknad_opprettet on soknad(opprettet);
