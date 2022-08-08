package com.stripe.android.ui.core.elements.autocomplete.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransformGoogleToStripeAddressTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `test US address without sublocality`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name" : "123",
                            "short_name" : "123",
                            "types" : [ "street_number" ]
                        },
                        {
                            "long_name" : "South Main Street",
                            "short_name" : "South Main St",
                            "types" : [ "route" ]
                        },
                        {
                            "long_name" : "Pioneer Square",
                            "short_name" : "Pioneer Square",
                            "types" : [ "neighborhood", "political" ]
                        },
                        {
                            "long_name" : "Seattle",
                            "short_name" : "Seattle",
                            "types" : [ "locality", "political" ]
                        },
                        {
                            "long_name" : "King County",
                            "short_name" : "King County",
                            "types" : [ "administrative_area_level_2", "political" ]
                        },
                        {
                            "long_name" : "Washington",
                            "short_name" : "WA",
                            "types" : [ "administrative_area_level_1", "political" ]
                        },
                        {
                            "long_name" : "United States",
                            "short_name" : "US",
                            "types" : [ "country", "political" ]
                        },
                        {
                            "long_name" : "98104",
                            "short_name" : "98104",
                            "types" : [ "postal_code" ]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "Seattle",
                country = "US",
                line1 = "123 South Main Street",
                line2 = null,
                postalCode = "98104",
                state = "WA"
            )
        )
    }

    @Test
    fun `test US address with sublocality`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name" : "123",
                            "short_name" : "123",
                            "types" : [ "street_number" ]
                        },
                        {
                            "long_name" : "East Broadway",
                            "short_name" : "E Broadway",
                            "types" : [ "route" ]
                        },
                        {
                            "long_name" : "Manhattan",
                            "short_name" : "Manhattan",
                            "types" : [ "sublocality_level_1", "sublocality", "political" ]
                        },
                        {
                            "long_name" : "New York",
                            "short_name" : "New York",
                            "types" : [ "locality", "political" ]
                        },
                        {
                            "long_name" : "New York County",
                            "short_name" : "New York County",
                            "types" : [ "administrative_area_level_2", "political" ]
                        },
                        {
                            "long_name" : "New York",
                            "short_name" : "NY",
                            "types" : [ "administrative_area_level_1", "political" ]
                        },
                        {
                            "long_name" : "United States",
                            "short_name" : "US",
                            "types" : [ "country", "political" ]
                        },
                        {
                            "long_name" : "10002",
                            "short_name" : "10002",
                            "types" : [ "postal_code" ]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "New York",
                country = "US",
                line1 = "123 East Broadway",
                line2 = null,
                postalCode = "10002",
                state = "NY"
            )
        )
    }

    @Test
    fun `test US address with null street number`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name" : "South Main Street",
                            "short_name" : "South Main St",
                            "types" : [ "route" ]
                        },
                        {
                            "long_name" : "Pioneer Square",
                            "short_name" : "Pioneer Square",
                            "types" : [ "neighborhood", "political" ]
                        },
                        {
                            "long_name" : "Seattle",
                            "short_name" : "Seattle",
                            "types" : [ "locality", "political" ]
                        },
                        {
                            "long_name" : "King County",
                            "short_name" : "King County",
                            "types" : [ "administrative_area_level_2", "political" ]
                        },
                        {
                            "long_name" : "Washington",
                            "short_name" : "WA",
                            "types" : [ "administrative_area_level_1", "political" ]
                        },
                        {
                            "long_name" : "United States",
                            "short_name" : "US",
                            "types" : [ "country", "political" ]
                        },
                        {
                            "long_name" : "98104",
                            "short_name" : "98104",
                            "types" : [ "postal_code" ]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "Seattle",
                country = "US",
                line1 = "South Main Street",
                line2 = null,
                postalCode = "98104",
                state = "WA"
            )
        )
    }

    @Test
    fun `test should make dependent locality line 2 - BR`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name": "512",
                            "short_name": "512",
                            "types": ["street_number"]
                        },
                        {
                            "long_name": "Rua Borges Lagoa",
                            "short_name": "R. Borges Lagoa",
                            "types": ["route"]
                        },
                        {
                            "long_name": "Vila Clementino",
                            "short_name": "Vila Clementino",
                            "types": ["sublocality_level_1", "sublocality", "political"]
                        },
                        {
                            "long_name": "São Paulo",
                            "short_name": "São Paulo",
                            "types": ["administrative_area_level_2", "political"]
                        },
                        {
                            "long_name": "São Paulo",
                            "short_name": "SP",
                            "types": ["administrative_area_level_1", "political"]
                        },
                        {
                            "long_name": "Brazil",
                            "short_name": "BR",
                            "types": ["country", "political"]
                        },
                        {
                            "long_name": "04038-000",
                            "short_name": "04038-000",
                            "types": ["postal_code"]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "São Paulo",
                country = "BR",
                line1 = "Rua Borges Lagoa 512",
                line2 = "Vila Clementino",
                postalCode = "04038-000",
                state = "SP"
            )
        )
    }

    @Test
    fun `test should not combine dependent locality - US`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name": "1231",
                            "short_name": "1231",
                            "types": ["street_number"]
                        },
                        {
                            "long_name": "116th Avenue Northeast",
                            "short_name": "116th Ave NE",
                            "types": ["route"]
                        },
                        {
                            "long_name": "Wilburton",
                            "short_name": "Wilburton",
                            "types": ["neighborhood", "political"]
                        },
                        {
                            "long_name": "Bellevue",
                            "short_name": "Bellevue",
                            "types": ["locality", "political"]
                        },
                        {
                            "long_name": "King County",
                            "short_name": "King County",
                            "types": ["administrative_area_level_2", "political"]
                        },
                        {
                            "long_name": "Washington",
                            "short_name": "WA",
                            "types": ["administrative_area_level_1", "political"]
                        },
                        {
                            "long_name": "United States",
                            "short_name": "US",
                            "types": ["country", "political"]
                        },
                        {
                            "long_name": "98004",
                            "short_name": "98004",
                            "types": ["postal_code"]
                        },
                        {
                            "long_name": "3804",
                            "short_name": "3804",
                            "types": ["postal_code_suffix"]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "Bellevue",
                country = "US",
                line1 = "1231 116th Avenue Northeast",
                line2 = null,
                postalCode = "98004",
                state = "WA"
            )
        )
    }

    @Test
    fun `test JP address`() {
        val place = parseJson(
            """
                {
                    "address_components": [
                        {
                            "long_name" : "1",
                            "short_name" : "1",
                            "types" : [ "premise" ]
                        },
                        {
                            "long_name" : "6",
                            "short_name" : "6",
                            "types" : [ "sublocality_level_4", "sublocality", "political" ]
                        },
                        {
                            "long_name" : "3-chōme",
                            "short_name" : "3-chōme",
                            "types" : [ "sublocality_level_3", "sublocality", "political" ]
                        },
                        {
                            "long_name" : "Kameido",
                            "short_name" : "Kameido",
                            "types" : [ "sublocality_level_2", "sublocality", "political" ]
                        },
                        {
                            "long_name" : "Koto City",
                            "short_name" : "Koto City",
                            "types" : [ "locality", "political" ]
                        },
                        {
                            "long_name" : "Tokyo",
                            "short_name" : "Tokyo",
                            "types" : [ "administrative_area_level_1", "political" ]
                        },
                        {
                            "long_name" : "Japan",
                            "short_name" : "JP",
                            "types" : [ "country", "political" ]
                        },
                        {
                            "long_name" : "136-0071",
                            "short_name" : "136-0071",
                            "types" : [ "postal_code" ]
                        }
                    ]
                }
            """.trimIndent()
        )

        val stripeAddress = place.transformGoogleToStripeAddress(context)

        assertThat(stripeAddress).isEqualTo(
            Address(
                city = "Koto City",
                country = "JP",
                line1 = "3-chōme-6-1 Kameido Koto City",
                line2 = null,
                postalCode = "136-0071",
                state = "Tokyo"
            )
        )
    }

    private fun parseJson(json: String): Place {
        return Json.decodeFromString(
            Place.serializer(),
            json
        )
    }
}
