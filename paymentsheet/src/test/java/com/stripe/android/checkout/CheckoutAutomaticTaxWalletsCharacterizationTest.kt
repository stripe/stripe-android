package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

/**
 * Characterization test for MOBILESDK-4667. MPE already disables wallets (Google Pay / Link)
 * when a Checkout Session has automatic tax enabled with the billing address as the tax source.
 * This locks in that behavior so the new billing-address-requirements utility does not regress it.
 */
@OptIn(CheckoutSessionPreview::class)
class CheckoutAutomaticTaxWalletsCharacterizationTest {

    @Test
    fun `wallets disabled when automatic tax uses billing address`() {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        assertThat(response.shouldDisableWalletsForAutomaticTaxBilling).isTrue()

        val initMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "test_key",
            checkoutSessionResponse = response,
        )

        assertThat(initMode.walletsDisabledReason())
            .isEqualTo(PaymentElementLoader.InitializationMode.WalletsDisabledReason.AutomaticTaxBillingAddress)
    }

    @Test
    fun `wallets not disabled when automatic tax uses shipping address`() {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = true,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.SHIPPING,
        )

        assertThat(response.shouldDisableWalletsForAutomaticTaxBilling).isFalse()

        val initMode = PaymentElementLoader.InitializationMode.CheckoutSession(
            instancesKey = "test_key",
            checkoutSessionResponse = response,
        )

        assertThat(initMode.walletsDisabledReason()).isNull()
    }

    @Test
    fun `wallets not disabled when automatic tax is off`() {
        val response = CheckoutSessionResponseFactory.create(
            automaticTaxEnabled = false,
            taxAddressSource = CheckoutSessionResponse.TaxAddressSource.BILLING,
        )

        assertThat(response.shouldDisableWalletsForAutomaticTaxBilling).isFalse()
    }
}
