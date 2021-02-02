package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.lenkeAltinnPortal
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettSMSNotification
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import java.time.format.DateTimeFormatter

class AltinnVarselMapper(val altinnTjenesteKode: String) {

    private fun opprettManglendeInnsendingNotifications(): NotificationBEList {
        val epost = opprettEpostNotification("Inntektsmelding mangler - sykepenger",
                "<p>Vi mangler inntektsmelding fra dere og kan ikke utbetale sykepenger. Sjekk Altinn for å se hvilke ansatte du må sende inntektsmelding for.</p>" +
                "<p>Vennlig hilsen NAV</p>")

        val sms = opprettSMSNotification(
                "Vi mangler inntektsmelding fra dere og kan ikke utbetale sykepenger. Sjekk Altinn for å se hvilke ansatte du må sende inntektsmelding for. \n\nVennlig hilsen NAV"
        )

        return NotificationBEList()
                .withNotification(epost, sms)
    }

    fun mapVarslingTilInsertCorrespondence(altinnVarsel: Varsling): InsertCorrespondenceV2 {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val tittel = "Inntektsmelding mangler - sykepenger"
        
        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <p>
                        Virksomhet: (${altinnVarsel.virksomhetsNr}).                      
                        </p>
                        <p>Det mangler inntektsmelding fra dere, og vi kan ikke utbetale sykepenger for følgende ansatte:</p>
                        ${altinnVarsel.liste.map { 
                        """
                            <p>
                            <strong>${it.navn}</strong><br>
                            Fødselsnummer: ${it.personnumer}<br>
                            Periode: ${it.periode.fom.format(formatter)} - ${it.periode.tom.format(formatter)}
                            </p>
                        """.trimIndent()
                        }.joinToString(separator = "\n")}
                        
                        <p>
                            For at vi skal kunne utbetale sykepengene, må dere sende inntektsmelding så snart som mulig.
                        </p>
                        
                        <p>
                            <a href="https://www.altinn.no/skjemaoversikt/arbeids--og-velferdsetaten-nav/Inntektsmelding-til-NAV/">Skjema for inntektsmelding (NAV 08-30.01) finner du her</a><br>
                            Benytter dere eget HR-system for å sende inntektsmeldinger kan dere fortsatt benytte dette.
                        </p>
                        
                       <h4>Om denne meldingen:</h4>
                       <p>Denne meldingen er automatisk generert og skal hjelpe arbeidsgivere med å få oversikt over inntektsmeldinger som mangler. NAV påtar seg ikke ansvar for eventuell manglende påminnelse. Vi garanterer heller ikke for at foreldelsesfristen ikke er passert, eller om det er andre grunner til at retten til sykepenger ikke er oppfylt. Dersom arbeidstakeren har åpnet en søknad og avbrutt den, vil det ikke bli sendt melding til dere.</p>
                   </div>
               </body>
            </html>
        """.trimIndent()

        val meldingsInnhold = ExternalContentV2()
                .withLanguageCode("1044")
                .withMessageTitle(tittel)
                .withMessageBody(innhold)
                .withMessageSummary("NAV mangler inntektsmelding for en, eller flere av deres ansatte for å kunne utbetale stønaderdet nylig er søkt om.")

        return InsertCorrespondenceV2()
                .withAllowForwarding(false)
                .withReportee(altinnVarsel.virksomhetsNr)
                .withMessageSender("NAV (Arbeids- og velferdsetaten)")
                .withServiceCode(altinnTjenesteKode)
                .withServiceEdition("1")
                .withNotifications(opprettManglendeInnsendingNotifications())
                .withContent(meldingsInnhold)
    }
}