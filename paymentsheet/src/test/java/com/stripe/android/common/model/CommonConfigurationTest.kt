package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiConfiguration
import com.stripe.android.ApiConfigurationPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Test

internal class CommonConfigurationTest {
    private val configuration = CommonConfigurationFactory.create()

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

    @OptIn(ApiConfigurationPreview::class)
    @Test
    fun `EmbeddedPaymentElement Configuration asCommonConfiguration maps apiConfiguration`() {
        val apiConfig = ApiConfiguration("pk_test_embedded")
        val embeddedConfig = EmbeddedPaymentElement.Configuration.Builder("Merchant")
            .apiConfiguration(apiConfig)
            .build()

        val common = embeddedConfig.asCommonConfiguration()

        assertThat(common.apiConfiguration).isEqualTo(apiConfig)
    }

    @OptIn(ApiConfigurationPreview::class)
    @Test
    fun `EmbeddedPaymentElement Configuration asCommonConfiguration defaults apiConfiguration to null`() {
        val embeddedConfig = EmbeddedPaymentElement.Configuration.Builder("Merchant").build()

        val common = embeddedConfig.asCommonConfiguration()

        assertThat(common.apiConfiguration).isNull()
    }

    @Test
    fun `PaymentSheet Configuration asCommonConfiguration has null apiConfiguration`() {
        val paymentSheetConfig = PaymentSheet.Configuration("Merchant")

        val common = paymentSheetConfig.asCommonConfiguration()

        assertThat(common.apiConfiguration).isNull()
    }
}
