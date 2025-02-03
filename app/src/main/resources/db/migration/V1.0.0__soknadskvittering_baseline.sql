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
    mottatteVedlegg jsonb,
    etterspurteVedlegg jsonb,
    opprettet timestamp with time zone not null,
    ferdigstilt timestamp with time zone
);

create index soknad_ident on soknadskvittering(ident);
create index soknad_opprettet on soknadskvittering(opprettet);
