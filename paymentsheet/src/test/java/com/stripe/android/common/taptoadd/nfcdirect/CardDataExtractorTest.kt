package com.stripe.android.common.taptoadd.nfcdirect

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.nfcdirect.TlvParser.hexToByteArray
import org.junit.Test

class CardDataExtractorTest {

    private val extractor = CardDataExtractor()

    @Test
    fun `extract card data from PAN and expiry tags`() {
        // Tag 5A (PAN) + Tag 5F24 (Expiry YYMMDD)
        val records = ("5A084111111111111111" + "5F2403261231").hexToByteArray()
        val aid = "A0000000031010".hexToByteArray() // Visa

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.pan).isEqualTo("4111111111111111")
        assertThat(cardData.expiryMonth).isEqualTo(12)
        assertThat(cardData.expiryYear).isEqualTo(2026)
        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.VISA)
    }

    @Test
    fun `extract card data from Track 2`() {
        // Tag 57 (Track 2): PAN D YYMM ServiceCode
        val records = "57124111111111111111D2612201123456789".hexToByteArray()
        val aid = "A0000000041010".hexToByteArray() // Mastercard

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.pan).isEqualTo("4111111111111111")
        assertThat(cardData.expiryMonth).isEqualTo(12)
        assertThat(cardData.expiryYear).isEqualTo(2026)
        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.MASTERCARD)
    }

    @Test
    fun `extract cardholder name`() {
        val records = ("5A084111111111111111" +
            "5F2403261231" +
            "5F200E4A4F484E20444F452F4D522E20").hexToByteArray()
        val aid = "A0000000031010".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.cardholderName).isEqualTo("JOHN DOE/MR.")
    }

    @Test
    fun `last4 property returns last 4 digits`() {
        val records = "5A084111111111111234".hexToByteArray()
        val aid = "A0000000031010".hexToByteArray()

        // Need valid expiry too
        val fullRecords = (records.toList() + "5F2403261231".hexToByteArray().toList()).toByteArray()
        val cardData = extractor.extract(fullRecords, aid)

        assertThat(cardData.last4).isEqualTo("1234")
    }

    @Test
    fun `detect Visa scheme from AID`() {
        val records = ("5A084111111111111111" + "5F2403261231").hexToByteArray()
        val aid = "A0000000031010".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.VISA)
    }

    @Test
    fun `detect Mastercard scheme from AID`() {
        val records = ("5A085555555555554444" + "5F2403261231").hexToByteArray()
        val aid = "A0000000041010".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.MASTERCARD)
    }

    @Test
    fun `detect Amex scheme from AID`() {
        val records = ("5A08378282246310005" + "5F2403261231").hexToByteArray()
        val aid = "A00000002501".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.AMEX)
    }

    @Test
    fun `detect Discover scheme from AID`() {
        val records = ("5A086011111111111117" + "5F2403261231").hexToByteArray()
        val aid = "A0000001523010".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        assertThat(cardData.scheme).isEqualTo(CardDataExtractor.CardScheme.DISCOVER)
    }

    @Test(expected = CardDataExtractionException::class)
    fun `throws when PAN not found`() {
        val records = "5F2403261231".hexToByteArray() // Only expiry, no PAN
        val aid = "A0000000031010".hexToByteArray()

        extractor.extract(records, aid)
    }

    @Test(expected = CardDataExtractionException::class)
    fun `throws when expiry not found`() {
        val records = "5A084111111111111111".hexToByteArray() // Only PAN, no expiry
        val aid = "A0000000031010".hexToByteArray()

        extractor.extract(records, aid)
    }

    @Test
    fun `extractAid finds AID in PPSE response`() {
        // Simulated PPSE response with Visa AID
        val ppseResponse = ("6F1A840E325041592E5359532E4444463031A5088801015F2D02656E" +
            "9000").hexToByteArray()

        // This is a simplified test - real PPSE parsing would need more data
        val aid = extractor.extractAid(ppseResponse)

        // Should find the AID
        assertThat(aid).isNotNull()
    }

    @Test
    fun `extractPdol returns PDOL from SELECT response`() {
        // Simulated SELECT AID response with PDOL (9F38)
        val selectResponse = ("6F208407A0000000031010A515500A564953412044454249549F380B" +
            "9F66049F02069F37049000").hexToByteArray()

        val pdol = extractor.extractPdol(selectResponse)

        // PDOL should be present (contains 9F66, 9F02, 9F37)
        assertThat(pdol).isNotNull()
    }

    @Test
    fun `extractAfl finds AFL in GPO response`() {
        // GPO Format 2 response with AFL
        val gpoResponse = "770E820219009408080101001001039000".hexToByteArray()

        val afl = extractor.extractAfl(gpoResponse)

        assertThat(afl).isNotNull()
    }

    @Test
    fun `handles PAN with padding F`() {
        // PAN with trailing F padding (odd length PAN)
        val records = ("5A084111111111111F" + "5F2403261231").hexToByteArray()
        val aid = "A0000000031010".hexToByteArray()

        val cardData = extractor.extract(records, aid)

        // Should strip the F padding
        assertThat(cardData.pan).isEqualTo("411111111111")
    }
}
