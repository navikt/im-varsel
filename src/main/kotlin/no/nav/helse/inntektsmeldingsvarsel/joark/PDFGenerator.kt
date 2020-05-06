package no.nav.helse.inntektsmeldingsvarsel.joark

import no.nav.helse.inntektsmeldingsvarsel.domene.varsling.Varsling
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

    fun lagPDF(varsling: Varsling): ByteArray {
        val doc = PDDocument()
        val page = PDPage()
        val font = PDType0Font.load(doc, this::class.java.classLoader.getResource(FONT_NAME).openStream())
        doc.addPage(page)
        val contentStream = PDPageContentStream(doc, page)
        contentStream.beginText()
        contentStream.setFont(font, FONT_SIZE)
        val mediaBox = page.mediaBox
        val startX = mediaBox.lowerLeftX + MARGIN_X
        val startY = mediaBox.upperRightY - MARGIN_Y
        contentStream.newLineAtOffset(startX, startY)
        contentStream.showText("Virksomhetsnummer: ${varsling.virksomhetsNr}")
        contentStream.newLineAtOffset(0F, -LINE_HEIGHT * 2)
        contentStream.showText("Personer:")
        varsling.liste.forEach {
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

}