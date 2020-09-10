package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.lenkeAltinnPortal
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettSMSNotification
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsVarselDbEntity

class AltinnPermisjonsVarselMapper(val altinnTjenesteKode: String) {

    fun mapVarslingTilInsertCorrespondence(altinnVarsel: PermisjonsVarselDbEntity): InsertCorrespondenceV2 {
        val tittel = "Feilutsendt melding om manglende inntektsmelding"
        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                   
                       <h2>Feilutsendt melding om manglende inntektsmelding</h2>
                       <p>
                            Du har fått en beskjed i Altinn fra NAV der vi etterlyser inntektsmelding. Dersom du allerede har sendt inn inntektsmeldingen, kan du se bort fra beskjeden fordi det har skjedd en feil hos oss. Vi tester for tiden en tjeneste for å gi arbeidsgivere beskjed om at inntektsmelding mangler. Ved en feil ble det sendt beskjed til for mange arbeidsgivere.
                        <p>
<p></p>                        
                        <p>
                        Vi beklager ulempen dette medfører.
                        </p>
                        
                        <p>
                        Hilsen NAV
                        </p>
                   </div>
               </body>
            </html>
        """.trimIndent()


        val meldingsInnhold = ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(tittel)
                .withMessageBody(innhold)
                .withMessageSummary("Feilutsendt melding om manglende inntektsmelding")

        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(altinnVarsel.virksomhetsNr)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(altinnTjenesteKode)
                .withServiceEdition("10")
                .withContent(meldingsInnhold)
    }
}