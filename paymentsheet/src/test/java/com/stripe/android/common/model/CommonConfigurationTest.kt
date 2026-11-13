package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkController
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.Rule
import org.junit.Test

internal class CommonConfigurationTest {
    @get:Rule
    val featureFlag = FeatureFlagTestRule(FeatureFlags.serverPaymentMethodAvailabilityEnabled, isEnabled = true)

    private val configuration = CommonConfigurationFactory.create()

    @Test
    fun `PaymentSheet common configuration opts into MobileSession`() {
        val commonConfiguration = PaymentSheet.Configuration("Merchant").asCommonConfiguration()

        assertThat(commonConfiguration.useRemoteMobileSession).isTrue()
    }

    @Test
    fun `Embedded common configuration opts into MobileSession`() {
        val commonConfiguration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
            .build()
            .asCommonConfiguration()

        assertThat(commonConfiguration.useRemoteMobileSession).isTrue()
    }

    @Test
    fun `PaymentSheet common configuration disables MobileSession when the flag is off`() {
        featureFlag.setEnabled(false)

        val commonConfiguration = PaymentSheet.Configuration("Merchant").asCommonConfiguration()

        assertThat(commonConfiguration.useRemoteMobileSession).isFalse()
    }

    @Test
    fun `Embedded common configuration disables MobileSession when the flag is off`() {
        featureFlag.setEnabled(false)

        val commonConfiguration = EmbeddedPaymentElement.Configuration.Builder("Merchant")
            .build()
            .asCommonConfiguration()

        assertThat(commonConfiguration.useRemoteMobileSession).isFalse()
    }

    @Test
    fun `Standalone Link has expected API configuration`() {
        val linkConfiguration = LinkController.Configuration(
            merchantDisplayName = "Example",
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
            stripeAccountId = DEFAULT_API_CONFIG.stripeAccountId,
            email = "jenny@example.com",
        ).build()

        val commonConfiguration = PaymentElementLoader.Configuration.StandaloneLink(linkConfiguration)
            .commonConfiguration

        assertThat(commonConfiguration.apiConfiguration).isEqualTo(DEFAULT_API_CONFIG)
        assertThat(commonConfiguration.useRemoteMobileSession).isFalse()
    }

    @Test
    fun `Crypto Onramp has expected API configuration`() {
        val linkConfiguration = LinkController.Configuration(
            merchantDisplayName = "Example",
            publishableKey = DEFAULT_API_CONFIG.publishableKey,
            stripeAccountId = DEFAULT_API_CONFIG.stripeAccountId,
        ).build()

        val commonConfiguration = PaymentElementLoader.Configuration.CryptoOnramp(linkConfiguration)
            .commonConfiguration

        assertThat(commonConfiguration.apiConfiguration).isEqualTo(DEFAULT_API_CONFIG)
        assertThat(commonConfiguration.useRemoteMobileSession).isFalse()
    }

    @Test
    fun `'containVolatileDifferences' should return false when no volatile differences are found`() {
        val changedConfiguration = configuration.copy(
            merchantDisplayName = "New merchant, Inc.",
        )

        assertThat(configuration.containsVolatileDifferences(changedConfiguration)).isFalse()
    }

    @Test
    fun `'containVolatileDifferences' should return true when volatile differences are found`() {
        val configWithCardBrandAcceptanceChanges = configuration.copy(
            cardBrandAcceptance = PaymentSheet.CardBrandAcceptance.disallowed(
                listOf(PaymentSheet.CardBrandAcceptance.BrandCategory.Visa)
            )
        )

        assertThat(configuration.containsVolatileDifferences(configWithCardBrandAcceptanceChanges)).isTrue()

        val configWithBillingDetailsChanges = configuration.copy(
            defaultBillingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Richards",
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingDetailsChanges)).isTrue()

        val configWithBillingConfigChanges = configuration.copy(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            ),
        )

        assertThat(configuration.containsVolatileDifferences(configWithBillingConfigChanges)).isTrue()
    }

    @Test
    fun `allowedCardFundingTypes returns configured list when enabled is true`() {
        val customFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
        val config = configuration.copy(
            allowedCardFundingTypes = customFundingTypes
        )

        assertThat(config.allowedCardFundingTypes(enabled = true))
            .isEqualTo(customFundingTypes)
    }

    @Test
    fun `allowedCardFundingTypes returns default list when enabled is false`() {
        val customFundingTypes = listOf(PaymentSheet.CardFundingType.Credit)
        val config = configuration.copy(
            allowedCardFundingTypes = customFundingTypes
        )

        assertThat(config.allowedCardFundingTypes(enabled = false))
            .isEqualTo(PaymentSheet.CardFundingType.entries)
    }
}
