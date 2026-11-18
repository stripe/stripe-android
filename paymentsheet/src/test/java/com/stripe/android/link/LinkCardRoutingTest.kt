package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.core.model.CountryCode
import com.stripe.android.model.LinkMode
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class LinkCardRoutingTest {
    @Test
    fun `route uses issuer country and leaves session unchanged`(@TestParameter case: RoutingCase) {
        val configuration = TestFactory.LINK_CONFIGURATION.copy(
            flags = case.flag?.let { mapOf("link_route_india_card_to_passthrough" to it) } ?: emptyMap(),
            passthroughModeEnabled = case.passthrough,
            linkMode = case.mode,
        )
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(country = case.country)

        assertThat(configuration.isIndiaCardOverride(card)).isEqualTo(case.isOverride)
        assertThat(configuration.shouldSharePaymentDetails(card)).isEqualTo(case.passthrough || case.isOverride)
        assertThat(configuration.passthroughModeEnabled).isEqualTo(case.passthrough)
        assertThat(configuration.linkMode).isEqualTo(case.mode)
    }

    @Test
    fun `India billing address does not route a non India issuer`() {
        val card = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.copy(
            country = "US",
            billingAddress = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD.billingAddress?.copy(
                countryCode = CountryCode("IN"),
            ),
        )
        assertThat(indiaConfiguration.shouldSharePaymentDetails(card)).isFalse()
    }

    @Test
    fun `bank account does not use India override`() {
        assertThat(indiaConfiguration.shouldSharePaymentDetails(TestFactory.CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT))
            .isFalse()
    }

    @Test
    fun `generic details do not use India override`() {
        assertThat(indiaConfiguration.shouldSharePaymentDetails(TestFactory.CONSUMER_PAYMENT_DETAILS_GENERIC))
            .isFalse()
    }

    private val indiaConfiguration = TestFactory.LINK_CONFIGURATION.copy(
        flags = mapOf("link_route_india_card_to_passthrough" to true),
        passthroughModeEnabled = false,
        linkMode = LinkMode.LinkPaymentMethod,
    )

    enum class RoutingCase(
        val country: String?,
        val flag: Boolean? = true,
        val mode: LinkMode? = LinkMode.LinkPaymentMethod,
        val passthrough: Boolean = false,
        val isOverride: Boolean = false,
    ) {
        Iso("IN", isOverride = true),
        Prefixed("COUNTRY_IN", isOverride = true),
        Lowercase("in", isOverride = true),
        LowercasePrefixed("country_in", isOverride = true),
        MixedCase("Country_In", isOverride = true),
        Missing(null),
        Blank(""),
        Whitespace(" "),
        Invalid("IND"),
        Unknown("COUNTRY_UNKNOWN"),
        NonIndia("US"),
        Disabled("IN", flag = false),
        MissingFlag("IN", flag = null),
        LegacyPaymentMethodMode("IN", mode = null, isOverride = true),
        ExplicitPassthrough("IN", mode = LinkMode.Passthrough),
        LinkCardBrand("IN", mode = LinkMode.LinkCardBrand),
        OrdinaryPassthrough("US", passthrough = true),
        IndiaInPassthrough("IN", passthrough = true),
    }
}
