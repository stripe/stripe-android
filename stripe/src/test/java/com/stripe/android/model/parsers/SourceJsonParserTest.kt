package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import org.junit.Test

class SourceJsonParserTest {

    @Test
    fun parse_shouldReturnExpectedObject() {
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
}
