package com.stripe.android.common.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import org.junit.Assert.fail
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
    fun `validateTermsDisplay throws exception for unsupported payment method type`() {
        val termsDisplay = mapOf(
            PaymentMethod.Type.Affirm to PaymentSheet.TermsDisplay.NEVER
        )

        val configuration = CommonConfigurationFactory.create(termsDisplay = termsDisplay)

        try {
            configuration.validate(isLiveMode = true)
            fail("Expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).isEqualTo("affirm does not support terms display configuration.")
        }
    }

    @Test
    fun `validateTermsDisplay works with empty termsDisplay map`() {
        val configuration = CommonConfigurationFactory.create(termsDisplay = emptyMap())

        configuration.validate(isLiveMode = true)
    }

    @Test
    fun `validateTermsDisplay does not throw an exception for both TermsDisplay values`() {
        val termsDisplayAutomatic = mapOf(
            PaymentMethod.Type.Card to PaymentSheet.TermsDisplay.AUTOMATIC
        )

        val termsDisplayNever = mapOf(
            PaymentMethod.Type.CashAppPay to PaymentSheet.TermsDisplay.NEVER
        )

        val configAutomatic = CommonConfigurationFactory.create(termsDisplay = termsDisplayAutomatic)
        val configNever = CommonConfigurationFactory.create(termsDisplay = termsDisplayNever)

        configAutomatic.validate(isLiveMode = true)
        configNever.validate(isLiveMode = true)
    }
}
