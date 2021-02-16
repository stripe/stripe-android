package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceJsonParserTest {

    @Test
    fun `should parse Klarna correctly`() {
        assertThat(SourceFixtures.KLARNA.klarna)
            .isEqualTo(
                Source.Klarna(
                    firstName = "Arthur",
                    lastName = "Dent",
                    purchaseCountry = "UK",
                    clientToken = "CLIENT_TOKEN",
                    payLaterAssetUrlsDescriptive = "https://x.klarnacdn.net/payment-method/assets/badges/generic/klarna.svg",
                    payLaterAssetUrlsStandard = "https://x.klarnacdn.net/payment-method/assets/badges/generic/klarna.svg",
                    payLaterName = "Pay later in 14 days",
                    payLaterRedirectUrl = "https://payment-eu.playground.klarna.com/8b45xe2",
                    payNowAssetUrlsDescriptive = null,
                    payNowAssetUrlsStandard = null,
                    payNowName = null,
                    payNowRedirectUrl = null,
                    payOverTimeAssetUrlsDescriptive = "https://x.klarnacdn.net/payment-method/assets/badges/generic/klarna.svg",
                    payOverTimeAssetUrlsStandard = "https://x.klarnacdn.net/payment-method/assets/badges/generic/klarna.svg",
                    payOverTimeName = "3 interest-free instalments",
                    payOverTimeRedirectUrl = "https://payment-eu.playground.klarna.com/8DA6imn",
                    paymentMethodCategories = setOf("pay_later", "pay_over_time"),
                    customPaymentMethods = emptySet()
                )
            )
    }

    @Test
    fun `should parse Redirect correctly`() {
        assertThat(SourceFixtures.REDIRECT)
            .isEqualTo(
                Source.Redirect(
                    returnUrl = "https://google.com",
                    status = Source.Redirect.Status.Succeeded,
                    url = "examplecompany://redirect-link"
                )
            )
    }

    @Test
    fun `should parse CodeVertification correctly`() {
        val codeVerification = SourceFixtures.SOURCE_CODE_VERIFICATION
        assertEquals(3, codeVerification.attemptsRemaining)
        assertEquals(Source.CodeVerification.Status.Pending, codeVerification.status)
    }

    @Test
    fun `should parse Receiver correctly`() {
        assertThat(SourceFixtures.SOURCE_RECEIVER)
            .isEqualTo(
                Source.Receiver(
                    address = "test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N",
                    amountCharged = 10,
                    amountReceived = 20,
                    amountReturned = 30
                )
            )
    }

    @Test
    fun `should parse Owner with verified fields correctly`() {
        assertThat(
            SourceJsonParser.OwnerJsonParser()
                .parse(SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS)
        ).isEqualTo(
            Source.Owner(
                address = Address(
                    line1 = "123 Market St",
                    line2 = "#345",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94107"
                ),
                email = "jenny.rosen@example.com",
                name = "Jenny Rosen",
                phone = "4158675309",
                verifiedAddress = Address(
                    line1 = "123 Market St",
                    line2 = "#345",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94107"
                ),
                verifiedEmail = "jenny.rosen@example.com",
                verifiedName = "Jenny Rosen",
                verifiedPhone = "4158675309"

            )
        )
    }

    @Test
    fun `should parse Owner without verified fields correctly`() {
        assertThat(
            SourceJsonParser.OwnerJsonParser()
                .parse(SourceFixtures.SOURCE_OWNER_WITH_NULLS)
        ).isEqualTo(
            Source.Owner(
                address = null,
                email = "jenny.rosen@example.com",
                name = "Jenny Rosen",
                phone = "4158675309",
                verifiedAddress = null,
                verifiedEmail = null,
                verifiedName = null,
                verifiedPhone = null
            )
        )
    }
}
