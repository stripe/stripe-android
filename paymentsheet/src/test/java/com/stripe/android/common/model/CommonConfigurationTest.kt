package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.link.LinkController
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

internal class CommonConfigurationTest {
    private val configuration = CommonConfigurationFactory.create()

    @Test
    fun `Standalone Link preserves and reuses API configuration across builds and conversions`() {
        val linkConfiguration = LinkController.Configuration(
            merchantDisplayName = "Example",
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_ACCOUNT_ID,
            email = "jenny@example.com",
        )
        val firstState = linkConfiguration.build()
        val secondState = linkConfiguration.build()

        assertThat(firstState.apiConfiguration.publishableKey).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        assertThat(firstState.apiConfiguration.stripeAccountId).isEqualTo(ApiKeyFixtures.FAKE_ACCOUNT_ID)
        assertThat(secondState.apiConfiguration).isSameInstanceAs(firstState.apiConfiguration)
        assertThat(PaymentElementLoader.Configuration.StandaloneLink(firstState).commonConfiguration.apiConfiguration)
            .isSameInstanceAs(firstState.apiConfiguration)
        assertThat(PaymentElementLoader.Configuration.StandaloneLink(secondState).commonConfiguration.apiConfiguration)
            .isSameInstanceAs(firstState.apiConfiguration)
    }

    @Test
    fun `Crypto Onramp preserves and reuses API configuration across builds and conversions`() {
        val linkConfiguration = LinkController.Configuration(
            merchantDisplayName = "Example",
            publishableKey = ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
            stripeAccountId = ApiKeyFixtures.FAKE_ACCOUNT_ID,
        )
        val firstState = linkConfiguration.build()
        val secondState = linkConfiguration.build()

        assertThat(firstState.apiConfiguration.publishableKey).isEqualTo(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        assertThat(firstState.apiConfiguration.stripeAccountId).isEqualTo(ApiKeyFixtures.FAKE_ACCOUNT_ID)
        assertThat(secondState.apiConfiguration).isSameInstanceAs(firstState.apiConfiguration)
        assertThat(PaymentElementLoader.Configuration.CryptoOnramp(firstState).commonConfiguration.apiConfiguration)
            .isSameInstanceAs(firstState.apiConfiguration)
        assertThat(PaymentElementLoader.Configuration.CryptoOnramp(secondState).commonConfiguration.apiConfiguration)
            .isSameInstanceAs(firstState.apiConfiguration)
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
