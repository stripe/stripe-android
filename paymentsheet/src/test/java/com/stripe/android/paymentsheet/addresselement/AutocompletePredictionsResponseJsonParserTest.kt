package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class AutocompletePredictionsResponseJsonParserTest {

    @Test
    fun `uses display_data when primary text absent`() {
        val json = JSONObject(
            """
            {
              "source": "google",
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "subtitle": "San Francisco, CA, USA",
                    "title": "554 Fillmore Street"
                  },
                  "place_id": "places/ChIJcznybKSAhYARlTOE-mb13JA"
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result.predictions).hasSize(1)
        assertThat(result.predictions.single()).isEqualTo(
            AutocompleteSuggestion(
                placeId = "places/ChIJcznybKSAhYARlTOE-mb13JA",
                primaryText = "554 Fillmore Street",
                secondaryText = "San Francisco, CA, USA",
                address = null,
            )
        )
    }

    @Test
    fun `prefers primary text over display_data`() {
        val json = JSONObject(
            """
            {
              "suggestions": [
                {
                  "address": null,
                  "display_data": {
                    "subtitle": "Ignored subtitle",
                    "title": "Ignored title"
                  },
                  "place_id": "place_123",
                  "primary_text": "123 Main St",
                  "secondary_text": "San Francisco, CA"
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result.predictions).hasSize(1)
        assertThat(result.predictions.single()).isEqualTo(
            AutocompleteSuggestion(
                placeId = "place_123",
                primaryText = "123 Main St",
                secondaryText = "San Francisco, CA",
                address = null,
            )
        )
    }

    @Test
    fun `returns empty list when suggestions key absent`() {
        val json = JSONObject("{}")

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result.predictions).isEmpty()
    }

    @Test
    fun `skips entries with missing place_id`() {
        val json = JSONObject(
            """
            {
              "suggestions": [
                {
                  "primary_text": "123 Main St",
                  "secondary_text": "SF, CA"
                },
                {
                  "place_id": "place_valid",
                  "primary_text": "456 Oak Ave",
                  "secondary_text": "LA, CA"
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result.predictions).hasSize(1)
        assertThat(result.predictions.single().placeId).isEqualTo("place_valid")
    }

    @Test
    fun `parses inline address`() {
        val json = JSONObject(
            """
            {
              "suggestions": [
                {
                  "place_id": "place_123",
                  "primary_text": "123 Main St",
                  "secondary_text": "SF, CA",
                  "address": {
                    "line1": "123 Main St",
                    "city": "San Francisco",
                    "state": "CA",
                    "postal_code": "94105",
                    "country": "US"
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val result = AutocompletePredictionsResponseJsonParser.parse(json)

        assertThat(result.predictions.single().address).isEqualTo(
            StripeProxyAddress(
                line1 = "123 Main St",
                line2 = null,
                city = "San Francisco",
                state = "CA",
                postalCode = "94105",
                country = "US",
            )
        )
    }
}
