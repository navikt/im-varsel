IM-Varsel 
================
TEST
Varsler arbeidsgivere om manglende inntektsmeldinger som hindrer saksgang.

Applikasjonen lytter på en kafkatopic der saksbehandlingssystemet for sykepengesaker
sender varsler om manglende inntektsmeldinger ifbm saksgang.

Disse blir så aggregert per virksomhet og tidsenhet, for eksempel per dag, og sendt til arbeidsgiver som en melding i Altinn.


# Bygg og testing

Appen er skrevet Kotlin og krever Java for å kjøre. 
For å bygge koden og kjøre enhetstester:
>./gradlew build test

For å kjøre integrasjonstester kreves noen avhengigheter som kjøres i Docker og Docker Compose. 
Start evhengighetene ved å kjøre 
>cd docker/local && docker-compose up

Når avhengighetene er oppe kan man kjøre integrasjonstester slik:
>./gradlew slowTests


# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #helse-arbeidsgiver
