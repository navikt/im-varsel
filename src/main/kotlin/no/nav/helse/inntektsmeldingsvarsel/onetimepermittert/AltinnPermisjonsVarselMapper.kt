package no.nav.helse.inntektsmeldingsvarsel.onetimepermittert

import no.altinn.schemas.services.serviceengine.correspondence._2010._10.ExternalContentV2
import no.altinn.schemas.services.serviceengine.correspondence._2010._10.InsertCorrespondenceV2
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.lenkeAltinnPortal
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettSMSNotification
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsVarselDbEntity
import java.time.format.DateTimeFormatter

class AltinnPermisjonsVarselMapper(val altinnTjenesteKode: String) {

    private fun opprettManglendeInnsendingNotifications(): NotificationBEList {
        val epost = opprettEpostNotification("Varsel om manglende opplysninger",
                "<p>NAV mangler opplysninger fra virksomheten \$reporteeName\$ for å kunne utbetale ytelser til deres ansatte.</p>" +
                        "<p>Logg inn på <a href=\"" + lenkeAltinnPortal() + "\">Altinn</a> for å lese mer om hva du må gjøre.</p>" +
                        "<p>Vennlig hilsen NAV</p>")

        val sms = opprettSMSNotification(
                "NAV mangler opplysninger fra virksomheten \$reporteeName\$ for å kunne utbetale ytelser til deres ansatte. Logg inn på Altinn for å lese mer om hva du må gjøre. \n\nVennlig hilsen NAV"
        )

        return NotificationBEList()
                .withNotification(epost, sms)
    }

    fun mapVarslingTilInsertCorrespondence(altinnVarsel: PermisjonsVarselDbEntity): InsertCorrespondenceV2 {
        val tittel = "NAV mangler opplysninger fra dere"
        
        val innhold = """
            <html>
               <head>
                   <meta charset="UTF-8">
               </head>
               <body>
                   <div class="melding">
                       <h2>NAV mangler opplysninger fra dere</h2>
                       <p>
                       <strong>
                       NAV trenger hjelp fra deg som arbeidsgiver for å utbetale lønnskompensasjon til dine ansatte som er eller har vært permitterte.
                       </strong>
                       </p>
                       
                        <p></p>
                        
                        <p>
                        39 000 arbeidsgivere har nå meldt inn opplysninger til NAV, slik at deres ansatte har fått utbetalt lønnskompensasjon. 
                        Vi mangler fremdeles innmelding fra noen arbeidsgivere. 
                        </p>
                        
                        <p>
                        <strong>
                        For at vi skal kunne utbetale lønnskompensasjon til deres ansatte må dere melde inn opplysninger i NAVs løsning for lønnskompensasjon og refusjon. 
                        </strong>
                        Denne innmeldingen utløser også refusjonen til arbeidsgivere som har forskuttert lønn til sine ansatte.  
                        </p>
                        
                        <p>
                        Så snart dere har gjort dette, utbetaler vi pengene i løpet av 2-3 virkedager.  
                        </p>
                        
                        <p>
                        Hvis dere allerede har meldt inn opplysninger til NAV kan dere se bort fra dette brevet.  
                        </p>
                        
                        <p>
                        <strong>
                        Har du spørsmål? 
                        </strong>
                        </p>
                       
                        <p>
                            Har du spørsmål om løsningen for lønnskompensasjon og refusjon kan du lese om løsningen <a href="https://arbeidsgiver.nav.no/permittering-refusjon/informasjon">her</a>. 
                            Finner du ikke svar her kan du kontakte oss på <a href="https://www.nav.no/kontakt">nav.no/kontakt</a>.                         
                        </p>
                            
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
                .withServiceEdition("10")
                .withNotifications(opprettManglendeInnsendingNotifications())
                .withContent(meldingsInnhold)
    }
}