create table soknadskvittering(
    soknadsId text primary key,
    ident text not null,
    tittel text not null,
    temakode text not null,
    skjemanummer text not null,
    tidspunktMottatt timestamp with time zone not null,
    fristEttersending date not null,
    linkSoknad text,
    journalpostId text,
    mottatteVedlegg jsonb not null,
    etterspurteVedlegg jsonb not null,
    produsent jsonb not null,
    opprettet timestamp with time zone not null,
    ferdigstilt timestamp with time zone,
    metadata jsonb
);

create index soknad_ident on soknadskvittering(ident);
create index soknadOpprettet on soknadskvittering(opprettet);
