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
        val epost = opprettEpostNotification("Varsel om manglende inntektsmelding",
                "<p>NAV mangler inntektsmelding for en, eller flere av deres ansatte på virksomheten \$reporteeName\$ for å kunne utbetale stønader det nylig er søkt om.</p>" +
                        "<p>Logg inn på <a href=\"" + lenkeAltinnPortal() + "\">Altinn</a> for å se hvem det gjelder og hvilken periode søknaden gjelder for.</p>" +
                        "<p>Vennlig hilsen NAV</p>")

        val sms = opprettSMSNotification(
                "NAV mangler inntektsmelding for en, eller flere av deres ansatte på virksomheten \$reporteeName\$ for å kunne utbetale stønader det nylig er søkt om. ",
                "Gå til meldingsboksen i Altinn for å se hvem det gjelder og hvilken periode søknaden gjelder for. \n\nVennlig hilsen NAV"
        )

        return NotificationBEList()
                .withNotification(epost, sms)
    }

    fun mapVarslingTilInsertCorrespondence(altinnVarsel: Varsling): InsertCorrespondenceV2 {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val tittel = "Beskjed om manglende inntektsmelding"
        
        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>Varsel om manglende inntektsmelding ifm. søknad om sykepenger</h2>
                       <p>
                        NAV mangler inntektsmelding for følgende ansatte ved virksomheten (${altinnVarsel.virksomhetsNr}). 
                        For at vi skal kunne utbetale sykepengene det søkes om må disse sendes oss så snart som mulig. 
                        Dersom dere har sendt inn disse i løpet av de siste 24 timene kan dere se bort fra dette varselet.
                        </p>
                        <p></p>
                        <p>
                            <a href="https://www.altinn.no/skjemaoversikt/arbeids--og-velferdsetaten-nav/Inntektsmelding-til-NAV/">Skjema for inntektsmelding (NAV 08-30.01) finner du her</a><br>
                            Benytter dere eget HR-system for å sende inntektsmeldinger kan dere fortsatt benytte dette.
                        </p>
                        ${altinnVarsel.liste.map { 
                        """
                            <p>
                            <strong>${it.navn}</strong><br>
                            Fødselsnummer: ${it.personnumer}<br>
                            Periode: ${it.periode.fom.format(formatter)} - ${it.periode.tom.format(formatter)}
                            </p>
                        """.trimIndent()
                        }.joinToString(separator = "\n")}
                        
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