package com.stripe.android.customersheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.PaymentSheet
import junit.framework.TestCase.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomerSheetConfigurationTest {
    @Test
    fun `Builder returns a configuration`() {
        val googlePayEnabled = true
        val merchantDisplayName = "Test"
        val appearance = PaymentSheet.Appearance().copy(
            typography = PaymentSheet.Typography.default
        )
        val billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
        )
        val defaultBillingDetails = PaymentSheet.BillingDetails(
            name = "Test"
        )
        val headerTextForSelectionScreen = "Test"
        val preferredNetworks = listOf(CardBrand.AmericanExpress)

        val configuration = CustomerSheet.Configuration.builder(merchantDisplayName)
            .googlePayEnabled(googlePayEnabled)
            .appearance(appearance)
            .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
            .defaultBillingDetails(defaultBillingDetails)
            .headerTextForSelectionScreen(headerTextForSelectionScreen)
            .preferredNetworks(preferredNetworks)
            .build()

        assertThat(configuration.googlePayEnabled)
            .isEqualTo(googlePayEnabled)
        assertThat(configuration.merchantDisplayName)
            .isEqualTo(merchantDisplayName)
        assertThat(configuration.appearance)
            .isEqualTo(appearance)
        assertThat(configuration.billingDetailsCollectionConfiguration)
            .isEqualTo(billingDetailsCollectionConfiguration)
        assertThat(configuration.defaultBillingDetails)
            .isEqualTo(defaultBillingDetails)
        assertThat(configuration.headerTextForSelectionScreen)
            .isEqualTo(headerTextForSelectionScreen)
        assertThat(configuration.preferredNetworks).isEqualTo(preferredNetworks)
    }

    @Test
    fun `newBuilder returns a new builder with previous configuration`() {
        val configuration = CustomerSheet.Configuration.builder(merchantDisplayName = "Example")
            .googlePayEnabled(true)
            .build()

        val configuration1 = configuration.newBuilder()
            .build()

        assertThat(configuration1.googlePayEnabled)
            .isTrue()

        val configuration2 = configuration.newBuilder()
            .googlePayEnabled(false)
            .build()

        assertThat(configuration2.googlePayEnabled)
            .isFalse()
    }

    @Test
    fun `Builder has the right methods`() {
        val knownBuilderMethodNames = setOf(
            "googlePayEnabled",
            "merchantDisplayName",
            "appearance",
            "billingDetailsCollectionConfiguration",
            "defaultBillingDetails",
            "headerTextForSelectionScreen",
            "preferredNetworks",
            "build",
            "allowsRemovalOfLastSavedPaymentMethod",
            "paymentMethodOrder",
            "cardBrandAcceptance",
            "updatePaymentMethodEnabled"
        )

        // Programmatically check for any new method on the builder using reflection
        val configurationMethods = CustomerSheet.Configuration.Builder::class.java.declaredMethods
        val newMethods = configurationMethods.map { it.name } - knownBuilderMethodNames

        // If there are any new methods, fail the test
        if (newMethods.isNotEmpty()) {
            fail(
                """
                New method added to the Builder class: ${newMethods.joinToString()}, please update
                this test and update CustomerSheet.Configuration#newBuilder
                """.trimIndent()
            )
        }
    }
}
