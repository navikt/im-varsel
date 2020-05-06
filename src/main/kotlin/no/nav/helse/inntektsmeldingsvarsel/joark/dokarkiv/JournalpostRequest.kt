package no.nav.helse.inntektsmeldingsvarsel.joark.dokarkiv

import java.time.LocalDate

data class JournalpostRequest(
        val tema: String = "SYK",
        val journalposttype: String = "UTGAAENDE",
        val bruker: Bruker,
        val avsenderMottaker: AvsenderMottaker,
        val tittel: String = "Varsel om manglende inntektsmelding",
        val kanal: String = "ALTINN",
        val journalfoerendeEnhet: String = "9999",
        val eksternReferanseId: String,
        val dokumenter: List<Dokument>,
        val sak: Sak = Sak(),
        val datoMottatt: LocalDate = LocalDate.now()
)

data class Bruker(
        val id: String,
        val idType: String = "ORGNR"
)

data class Dokument(
        val brevkode: String = "varsel_om_manglende_inntektsmelding",
        val tittel: String = "Varsel om manglende inntektsmelding",
        val dokumentVarianter: List<DokumentVariant>
)

data class DokumentVariant(
        val filtype: String = "PDFA",
        val fysiskDokument: String,
        val variantFormat: String = "ARKIV"
)

data class AvsenderMottaker(
        val id: String,
        val idType: String = "ORGNR"
)

data class Sak(
        val sakstype: String = "GENERELL_SAK"
)