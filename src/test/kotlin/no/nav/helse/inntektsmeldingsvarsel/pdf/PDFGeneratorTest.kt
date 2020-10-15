package no.nav.helse.inntektsmeldingsvarsel.pdf

import no.nav.helse.TestData
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PDFGeneratorTest {

    @Test
    fun testLagPDFVarsling() {
        val pdf = PDFGenerator().lagPDF(TestData.Varsling, TestData.Varsling.liste)
        assertThat(pdf).isNotNull()

        val allTextInDocument = extractTextFromPdf(pdf)

        TestData.Varsling.liste.forEach{
            assertThat(allTextInDocument).contains(it.navn)
            assertThat(allTextInDocument).contains(it.personnumer)
        }
    }

    @Test
    fun testLagPDFBrev() {
        val pdf = PDFGenerator().lagPDF(TestData.AltinnBrevmal)
        assertThat(pdf).isNotNull()

        val allTextInDocument = extractTextFromPdf(pdf)

        assertThat(allTextInDocument).contains(TestData.AltinnBrevmal.header)
        assertThat(allTextInDocument).contains(TestData.AltinnBrevmal.bodyHtml)
    }

    private fun extractTextFromPdf(pdf: ByteArray): String? {
        val pdfReader = PDDocument.load(pdf)
        val pdfStripper = PDFTextStripper()
        val allTextInDocument = pdfStripper.getText(pdfReader)
        pdfReader.close()
        return allTextInDocument
    }
}