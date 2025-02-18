# Min-side oversikt over innsendte søknader (Under arbeid)

1. Kafka tilgang: Opprett en pull-request mot topic `aapen-soknadskvittering-v1`
   i [min-side-soknadskvittering-topic-iac](https://github.com/navikt/min-side-soknadskvittering-topic-iac).
2. Koble på topicene.
3. Send event!

## Beskrivelse og flyt

Topic _min-side-soknadskvittering-topic-iac_ er ment for hendelser i søknader som har skjedd i Navs fagsystem. 
Hendelsene samles av appen [tms-soknadskvittering](https://github.com/navikt/tms-soknadskvittering), slik at det kan
presenteres for bruker en sentral plass.

I skrivende stund støtter systemet følgende eventer:

- `soknadOpprettet`: Innledende event med informasjon som er umiddelbart tilgjengelig (Tittel, arkivtema, skjemanummer etc).
- `soknadOppdatert`: Event med forsinket informasjon, som for eksempel lenke til pdf av søknaden.
- `soknadFerdigstilt`: Event som melder at søknaden på ett eller annet vis er ferdigbehandlet.
- `vedleggEtterspurt`: Event som inntreffer dersom bruker må ettersende ytterligere informasjon. 
- `vedleggMottatt`: Nav har mottatt vedlegg av bruker eller tredjepart.
- `vedleggOppdatert`: Informasjon om vedlegg er oppdatert, for eksempel lenke til pdf.

## Kafka, schemas og buildere

Min side varsler bruker ikke lenger Avro for schema-validering og serialisering. Produsenter sender eventer direkte på json-format.

Vi tilbyr et sett med buildere for kotlin-prosjekter. Builderne sørger for at format er riktig og gjør forhåndsvalidering av innhold. Det er anbefalt å bruke søknadsId som kafka-nøkkel for alle eventer, for å opprettholde kronologi per enkelt søknad.

Builderne finnes i følgende bibliotek: `no.nav.tms.soknadskvittering:kotlin-builder:0.1.1`

Vi publiserer disse artifaktene til githubs package-repository. Husk å legge til én av disse repositories i ditt prosjekt:

- `https://maven.pkg.github.com/navikt/tms-soknadskvittering` (krever autentisering)
- `https://github-package-registry-mirror.gc.nav.no/cached/maven-release` (NAIS sin mirror. Krever ingen autentisering)

### Bruk av buildere og KafkaProducer

Buildere produserer kafka-event på json-format som kan legges rett på kafka. Merk at KafkaProducer må være konfigurert med StringSerializer.

Eksempel:

```kotlin
val producer: KafkaProducer<String, String>
val topic = "min-side.aapen-soknadskvittering-v1"

fun createAndSend(soknadsId: String) {
    val event = SoknadEventBuilder.opprettet {
        this.soknadsId = soknadsId
        //...
    }

   producer.send(ProducerRecord(topic, soknadsId, event))
}
```

## Beskrivelse av søknad-eventer

### Søknad opprettet

Skal sendes når Nav mottar en søknad fra bruker

| felt               | påkrevd          | beskrivelse                                                                  | restriksjoner                                                         |
|--------------------|------------------|------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| soknadsId          | ja               | Id til søknaden                                                              | Må være UUID eller ULID                                               |
| ident              | ja               | Fodselsnummer (evt. d-nummer eller tilsvarende) til bruker søknaden tilhører | Må ha 11 siffer                                                       |
| tittel             | ja               | Tittel på søknaden                                                           | Maks 80 tegn                                                          |
| temakode           | ja, minst 1      | Søknadens arkivtemakode                                                      | Maks 3 tegn                                                           |
| skjemanummer       | ikke for beskjed | Søknadens skjemanummer                                                       | Maks 40 tegn                                                          |
| tidspunktMottatt   | ja               | Tidspunktet Nav først mottok søknad fra bruker                               |                                                                       |
| fristEttersending  | ja               | Dato med frist for ettersending av vedlegg                                   |                                                                       |
| linkSoknad         | nei              | Link til kopi av søknaden der dette er tilgjengelig                          | Må være gyldig lenke og maks 200 tegn                                 |
| journalpostId      | nei              | Evt journalpostId fra SAF                                                    | Maks 20 tegn                                                          |
| mottatteVedlegg    | nei              | Liste over vedlegg som ble mottatt sammen med søknaden                       | VedleggsId må være unikt på tvers av mottatte og etterspurte vedlegg  |
| etterspurteVedlegg | nei              | Liste over vedlegg som mangler i det bruker sendte inn søknaden              |                                                                       |


Praktisk eksempel med kotlin builder. Her er journalpostId og lenker til søknad og vedlegg er ikke enda klart. Bruker har også meldt at legen sender inn ytterligere informasjon.

```kotlin

SoknadEventBuilder.opprettet {
   soknadsId = "<uuid>"
   ident = "01234567890"
   tittel = "Tittel på en søknad"
   temakode = "UKJ"
   skjemanummer = "Skjemanummer-123"
   fristEttersending = LocalDate.now().plusDays(14)
   mottattVedlegg {
       vedleggsId = "vedlegg-1"
       tittel = "Vedlegg som bruker sendte inn sammen med søknad"
   }
   etterspurteVedlegg {
       vedleggsId = "vedlegg-2"
       brukerErAvsender = false
       tittel = "Vedlegg som tredjepart sender inn"
       beskrivelse = "Bruker har meldt at legen sender dette vedlegget til Nav"
   }
   tidspunktMottatt = ZonedDateTime.now()
}
```


### Søknad oppdatert

Tillater endringer av bestemte felt etter søknad er opprettet. Dersom et felt er `null` blir ingen endring gjort. Dvs at en kan endre felt fra `null`, men ikke til `null`.

| felt              | påkrevd | beskrivelse                                         | restriksjoner                         |
|-------------------|---------|-----------------------------------------------------|---------------------------------------|
| soknadsId         | ja      | Id til søknaden                                     | Må være UUID eller ULID               |
| fristEttersending | nei     | Dato med frist for ettersending av vedlegg          |                                       |
| linkSoknad        | nei     | Link til kopi av søknaden der dette er tilgjengelig | Må være gyldig lenke og maks 200 tegn |
| journalpostId     | nei     | Evt journalpostId fra SAF                           | Maks 20 tegn                          |


Praktisk eksempel med kotlin builder. Her blir søknad oppdatert etter pdf er ferdig generert.

```kotlin

SoknadEventBuilder.oppdatert {
   soknadsId = "<uuid>"
   linkSoknad = "https://soknader.nav.no/soknad.pdf"
}
```

### Søknad ferdigstilt

Melder fra at søknad er ferdigstilt og ikke lenger vises til bruker

| felt              | påkrevd | beskrivelse                                         | restriksjoner                         |
|-------------------|---------|-----------------------------------------------------|---------------------------------------|
| soknadsId         | ja      | Id til søknaden                                     | Må være UUID eller ULID               |
| fristEttersending | nei     | Dato med frist for ettersending av vedlegg          |                                       |
| linkSoknad        | nei     | Link til kopi av søknaden der dette er tilgjengelig | Må være gyldig lenke og maks 200 tegn |
| journalpostId     | nei     | Evt journalpostId fra SAF                           | Maks 20 tegn                          |


Praktisk eksempel med kotlin builder.

```kotlin
SoknadEventBuilder.ferdigstilt {
   soknadsId = "<uuid>"
}
```



## Beskrivelse av vedlegg-eventer

### Vedlegg etterspurt bruker

Sendes når Nav krever ytterligere vedlegg for en søknad av bruker

| felt                | påkrevd | beskrivelse                                   | restriksjoner                                                                  |
|---------------------|---------|-----------------------------------------------|--------------------------------------------------------------------------------|
| soknadsId           | ja      | Id til søknaden                               | Må være UUID eller ULID                                                        |
| vedleggsId          | ja      | Id til vedlegg                                | Må være unik innen én søknad. Kan ikke etterspørre samme vedlegg flere ganger  |
| brukerErAvsender    | ja      | Om bruker eller tredjepart skal sende vedlegg |                                                                                |
| tittel              | ja      | Tittel på etterspurt vedlegg                  | Maks 255 tegn                                                                  |
| linkEttersending    | ja      | Link til der bruker skal ettersende vedlegg   | Gyldig lenke og maks 200 tegn                                                  |
| tidspunktEtterspurt | ja      | Dato med frist for ettersending av vedlegg    |                                                                                |


Praktisk eksempel med kotlin builder.

```kotlin
SoknadEventBuilder.vedleggEtterspurtBruker {
   soknadsId = "<uuid>"
   vedleggsId = "vedlegg-3"
   tittel = "Annet vedlegg som bruker må sende inn"
   linkEttersending = "https://link.til.ettersending"
   tidspunktEtterspurt = ZonedDateTime.now()
}
```

### Vedlegg etterspurt tredjepart

Sendes når Nav krever ytterligere vedlegg for en søknad av en tredjepart

| felt                | påkrevd                      | beskrivelse                                                     | restriksjoner                                                                  |
|---------------------|------------------------------|-----------------------------------------------------------------|--------------------------------------------------------------------------------|
| soknadsId           | ja                           | Id til søknaden                                                 | Må være UUID eller ULID                                                        |
| vedleggsId          | ja                           | Id til vedlegg                                                  | Må være unik innen én søknad. Kan ikke etterspørre samme vedlegg flere ganger  |
| brukerErAvsender    | ja                           | Om bruker eller tredjepart skal sende vedlegg                   |                                                                                |
| tittel              | ja                           | Tittel på etterspurt vedlegg                                    | Maks 255 tegn                                                                  |
| beskrivelse         | nei                          | Mer utfyllende informasjon om vedlegg tredjepart skal sende inn | Maks 500 tegn                                                                  |
| tidspunktEtterspurt | ja                           | Dato med frist for ettersending av vedlegg                      |                                                                                |


Praktisk eksempel med kotlin builder.

```kotlin
SoknadEventBuilder.vedleggEtterspurtTredjepart {
   soknadsId = "<uuid>"
   vedleggsId = "vedlegg-4"
   tittel = "Brukers arbeidsted må sende inn mer informasjon"
   tidspunktEtterspurt = ZonedDateTime.now()
}
```

### Vedlegg mottatt

Sendes når Nav mottar vedlegg. Dersom det finnes et etterspurt vedlegg med samme id, blir dette markert som mottatt.

| felt             | påkrevd | beskrivelse                                      | restriksjoner                                                           |
|------------------|---------|--------------------------------------------------|-------------------------------------------------------------------------|
| soknadsId        | ja      | Id til søknaden                                  | Må være UUID eller ULID                                                 |
| vedleggsId       | ja      | Id til vedlegg                                   | Må være unik innen én søknad. Kan ikke motta samme vedlegg flere ganger |
| tittel           | ja      | Tittel på mottatt vedlegg                        | Maks 255 tegn                                                           |
| linkVedlegg      | nei     | Link til kopi av vedlegg                         | Gyldig lenke og maks 200 tegn                                           |
| brukerErAvsender | ja      | Om bruker eller tredjepart er kilden til vedlegg |                                                                         |
| tidspunktMottatt | ja      | Tidspunkt Nav mottok vedlegg                     |                                                                         |


Praktiske eksempel med kotlin builder der bruker har sendt inn et vedlegg.

```kotlin
SoknadEventBuilder.vedleggMottatt {
   soknadsId = "<uuid>"
   vedleggsId = "vedlegg-3"
   tittel = "Vedlegg"
   linkVedlegg = "https://soknad.nav.no/vedlegg/vedlegg.pdf"
   tidspunktMottatt = ZonedDateTime.now()
}
```

### Vedlegg oppdatert

Tillater endringer av bestemte felt etter vedlegg er mottatt. Dersom et felt er `null` blir ingen endring gjort. Dvs at en kan endre felt fra `null`, men ikke til `null`.

I skrivende stund kan en kun oppdatere lenke til vedlegg.

| felt        | påkrevd | beskrivelse              | restriksjoner                         |
|-------------|---------|--------------------------|---------------------------------------|
| soknadsId   | ja      | Id til søknaden          | Må være UUID eller ULID               |
| vedleggsId  | ja      | Id til vedlegg           | Må matche et mottatt vedlegg.         |
| linkVedlegg | nei     | Link til kopi av vedlegg | Må være gyldig lenke og maks 200 tegn |


Praktisk eksempel med kotlin builder. Her blir vedlegg oppdatert etter pdf er ferdig generert.

```kotlin

SoknadEventBuilder.vedleggOppdatert {
   soknadsId = "<uuid>"
   linkSoknad = "https://soknader.nav.no/soknad.pdf"
}
```

### Søknad ferdigstilt

Melder fra at søknad er ferdigstilt og ikke lenger vises til bruker

| felt              | påkrevd | beskrivelse                                         | restriksjoner                         |
|-------------------|---------|-----------------------------------------------------|---------------------------------------|
| soknadsId         | ja      | Id til søknaden                                     | Må være UUID eller ULID               |
| fristEttersending | nei     | Dato med frist for ettersending av vedlegg          |                                       |
| linkSoknad        | nei     | Link til kopi av søknaden der dette er tilgjengelig | Må være gyldig lenke og maks 200 tegn |
| journalpostId     | nei     | Evt journalpostId fra SAF                           | Maks 20 tegn                          |


Praktisk eksempel med kotlin builder.

```kotlin
SoknadEventBuilder.ferdigstilt {
   soknadsId = "<uuid>"
}
```

## Produsent og buildere

Builderene vil forøke forsøke å utlede hvilket system som produserer eventer basert på miljøvariablene [`NAIS_CLUSTER_NAME`, `NAIS_NAMESPACE`, `NAIS_APP_NAME`].

For apper som kjører på nais vil disse være satt automatisk. Hvis en ønsker å kjøre tester med automatisk henting av produsent,
kan en manuelt legge til disse variablene ved hjelp av `BuilderEnvironment.extend(<map med variabler>)`.
