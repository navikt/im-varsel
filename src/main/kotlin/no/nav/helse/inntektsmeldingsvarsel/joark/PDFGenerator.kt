package no.nav.helse.inntektsmeldingsvarsel.joark

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.PersonVarsling
import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
import no.nav.helse.inntektsmeldingsvarsel.onetimepermittert.permisjonsvarsel.repository.PermisjonsVarselDbEntity
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PDFGenerator {

    private val FONT_SIZE = 11f
    private val LINE_HEIGHT = 15f
    private val MARGIN_X = 40f
    private val MARGIN_Y = 40f
    private val FONT_NAME = "fonts/SourceSansPro-Regular.ttf"
    val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun lagPDF(varsling: Varsling, personvarslinger: Set<PersonVarsling>): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val contentStream = PDPageContentStream(doc, page)
        contentStream.beginText()
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        contentStream.newLineAtOffset(startX, startY)
        contentStream.setFont(font, FONT_SIZE + 4)
        contentStream.showText("Melding om manglende inntektsmelding")
        contentStream.setFont(font, FONT_SIZE)
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 4)
        contentStream.showText("Virksomhetsnummer: ${varsling.virksomhetsNr}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 2)
        contentStream.showText("Personer:")
        personvarslinger.forEach {
            contentStream.newLineAtOffset(0F, -LINE_HEIGHT)
            contentStream.showText("${it.navn} (${it.personnumer}) for perioden ${DATE_FORMAT.format(it.periode.fom)} - ${DATE_FORMAT.format(it.periode.fom)}")
        }
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 2)
        contentStream.showText("Opprettet: ${TIMESTAMP_FORMAT.format(LocalDateTime.now())}")
        contentStream.endText()
        contentStream.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        val ba = out.toByteArray()
        doc.close()
        return ba
    }

    fun lagPDF(varsling: PermisjonsVarselDbEntity): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val contentStream = PDPageContentStream(doc, page)
        contentStream.beginText()
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        contentStream.newLineAtOffset(startX, startY)
        contentStream.setFont(font, FONT_SIZE + 4)
        contentStream.showText("Melding om manglende opplysninger")
        contentStream.setFont(font, FONT_SIZE)
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 4)
        contentStream.showText("NAV trenger hjelp fra deg som arbeidsgiver for å utbetale lønnskompensasjon til dine ansatte som er eller har vært permitterte." +
                "Ca. 3939 000 arbeidsgivere har nå meldt inn opplysninger til NAV, slik at deres ansatte har fått utbetalt lønnskompensasjon. Vi mangler fremdeles innmelding fra noen arbeidsgivere." +
                "For at vi skal kunne utbetale lønnskompensasjon til deres ansatte må dere melde inn opplysninger i NAVs løsning for lønnskompensasjon og refusjon. Denne innmeldingen utløser også refusjonen til arbeidsgivere som har forskuttert lønn til sine ansatte. "+
                "Så snart dere har gjort dette, utbetaler vi pengene i løpet av 2-3 virkedager." +
                "Hvis dere allerede har meldt inn opplysninger til NAV kan dere se bort fra dette brevet." +
                "Har du spørsmål?" +
                "Har du spørsmål om løsningen for lønnskompensasjon og refusjon kan du lese om løsningen her. Finner du ikke svar her kan du kontakte oss på www.nav.no/kontakt. ")
        contentStream.endText()
        contentStream.close()
        val out = ByteArrayOutputStream()
        doc.save(out)
        val ba = out.toByteArray()
        doc.close()
        return ba
    }

}
