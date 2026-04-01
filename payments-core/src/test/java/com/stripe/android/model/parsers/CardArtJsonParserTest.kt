package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import org.json.JSONObject
import org.junit.Test

class CardArtJsonParserTest {

    @Test
    fun `parse with all fields should return expected CardArt`() {
        val cardArt = CardArtJsonParser().parse(CARD_ART_JSON)

        assertThat(cardArt).isEqualTo(
            PaymentMethod.Card.CardArt(
                artImage = PaymentMethod.Card.CardArt.ArtImage(
                    format = "image/png",
                    url = "https://example.com/card_art.png"
                ),
                programName = "My Card Program",
                status = PaymentMethod.Card.CardArt.Status.Available
            )
        )
    }

    @Test
    fun `parse with null art_image should return CardArt with null artImage`() {
        val json = JSONObject(
            """
            {
                "program_name": "My Card Program",
                "status": "unavailable"
            }
            """.trimIndent()
        )

        val cardArt = CardArtJsonParser().parse(json)

        assertThat(cardArt).isNotNull()
        assertThat(cardArt!!.artImage).isNull()
        assertThat(cardArt.programName).isEqualTo("My Card Program")
        assertThat(cardArt.status).isEqualTo(PaymentMethod.Card.CardArt.Status.Unavailable)
    }

    @Test
    fun `parse with unknown status should return null`() {
        val json = JSONObject(
            """
            {
                "program_name": "My Card Program",
                "status": "pending"
            }
            """.trimIndent()
        )

        val cardArt = CardArtJsonParser().parse(json)

        assertThat(cardArt).isNull()
    }

    @Test
    fun `parse with missing status should return null`() {
        val json = JSONObject(
            """
            {
                "program_name": "My Card Program"
            }
            """.trimIndent()
        )

        val cardArt = CardArtJsonParser().parse(json)

        assertThat(cardArt).isNull()
    }

    @Test
    fun `parse art_image with missing format returns null artImage`() {
        val json = JSONObject(
            """
            {
                "art_image": {
                    "url": "https://example.com/card_art.png"
                },
                "status": "available"
            }
            """.trimIndent()
        )

        val cardArt = CardArtJsonParser().parse(json)

        assertThat(cardArt).isNotNull()
        assertThat(cardArt!!.artImage).isNull()
    }

    private companion object {
        val CARD_ART_JSON = JSONObject(
            """
            {
                "art_image": {
                    "format": "image/png",
                    "url": "https://example.com/card_art.png"
                },
                "program_name": "My Card Program",
                "status": "available"
            }
            """.trimIndent()
        )
    }
}
