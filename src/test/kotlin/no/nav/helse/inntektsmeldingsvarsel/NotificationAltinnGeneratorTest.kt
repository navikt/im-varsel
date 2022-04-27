package no.nav.helse.inntektsmeldingsvarsel

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType
import org.junit.jupiter.api.Test
import no.nav.helse.inntektsmeldingsvarsel.NotificationAltinnGenerator.opprettEpostNotification
import org.junit.jupiter.api.Assertions.assertEquals

internal class NotificationAltinnGeneratorTest {

    @Test
    fun opprettEpostNotification() {
        val epost = opprettEpostNotification(
            "Inntektsmelding mangler - sykepenger",
            "<p>NAV mangler inntektsmelding for en eller flere av deres ansatte i \$reporteeName\$. Vi får ikke utbetalt penger før inntektsmeldingen er sendt. Gå til meldingsboksen i Altinn for å se hvem det gjelder, og hvilken periode det handler om.</p>" +
                "<p>Vennlig hilsen NAV</p>"
        )
        assertEquals("noreply@altinn.no", epost.fromAddress)
        assertEquals("1044", epost.languageCode)
        assertEquals("TokenTextOnly", epost.notificationType)
        assertEquals(2, epost.textTokens.textToken.size)
        assertEquals(TransportType.EMAIL, epost.receiverEndPoints.receiverEndPoint[0].transportType)
    }

    @Test
    fun opprettSMSNotification() {
        val sms = NotificationAltinnGenerator.opprettSMSNotification(
            "NAV mangler inntektsmelding for en eller flere av deres ansatte i: \$reporteeName\$.",
            "Vi får ikke utbetalt penger før inntektsmeldingen er sendt. Gå til meldingsboksen i Altinn for å se hvem det gjelder, og hvilken periode det handler om. \n\nVennlig hilsen NAV"
        )
        assertEquals(null, sms.fromAddress)
        assertEquals("1044", sms.languageCode)
        assertEquals("TokenTextOnly", sms.notificationType)
        assertEquals(2, sms.textTokens.textToken.size)
        assertEquals(TransportType.SMS, sms.receiverEndPoints.receiverEndPoint[0].transportType)
    }
}
