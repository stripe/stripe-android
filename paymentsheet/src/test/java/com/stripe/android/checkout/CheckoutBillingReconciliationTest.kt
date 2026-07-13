package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Reconciliation of the CheckoutSession's billing_address_collection with the SDK's
 * BillingDetailsCollectionConfiguration.address. Mirrors iOS CheckoutAddressMergingTests.
 */
@OptIn(CheckoutSessionPreview::class)
class CheckoutBillingReconciliationTest {

    @Test
    fun `paymentSheet - required + Automatic upgrades address to Full`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Automatic))
        val state = state(requiresBillingAddress = true)

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `paymentSheet - required + Full stays Full`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Full))
        val state = state(requiresBillingAddress = true)

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `paymentSheet - auto + Automatic stays Automatic`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Automatic))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Automatic)
    }

    @Test
    fun `paymentSheet - auto + Full stays Full`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Full))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `paymentSheet - Never with required throws IllegalArgumentException`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Never))
        val state = state(requiresBillingAddress = true)

        val ex = assertFailsWith<IllegalArgumentException> {
            CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)
        }
        assertThat(ex.message).contains(
            "BillingDetailsCollectionConfiguration.address must not be CollectionMode.Never"
        )
    }

    @Test
    fun `paymentSheet - Never with auto throws IllegalArgumentException`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(address = Never))
        val state = state(requiresBillingAddress = false)

        assertFailsWith<IllegalArgumentException> {
            CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)
        }
    }

    @Test
    fun `paymentSheet - attachDefaultsToPaymentMethod is true after reconciliation`() {
        val config = paymentSheetConfiguration(bdcc = bdcc(attachDefaultsToPaymentMethod = false))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.PaymentSheetConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod).isTrue()
    }

    @Test
    fun `embedded - required + Automatic upgrades address to Full`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Automatic))
        val state = state(requiresBillingAddress = true)

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `embedded - required + Full stays Full`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Full))
        val state = state(requiresBillingAddress = true)

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `embedded - auto + Automatic stays Automatic`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Automatic))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Automatic)
    }

    @Test
    fun `embedded - auto + Full stays Full`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Full))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.address).isEqualTo(Full)
    }

    @Test
    fun `embedded - Never with required throws IllegalArgumentException`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Never))
        val state = state(requiresBillingAddress = true)

        val ex = assertFailsWith<IllegalArgumentException> {
            CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)
        }
        assertThat(ex.message).contains(
            "BillingDetailsCollectionConfiguration.address must not be CollectionMode.Never"
        )
    }

    @Test
    fun `embedded - Never with auto throws IllegalArgumentException`() {
        val config = embeddedConfiguration(bdcc = bdcc(address = Never))
        val state = state(requiresBillingAddress = false)

        assertFailsWith<IllegalArgumentException> {
            CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)
        }
    }

    @Test
    fun `embedded - attachDefaultsToPaymentMethod is true after reconciliation`() {
        val config = embeddedConfiguration(bdcc = bdcc(attachDefaultsToPaymentMethod = false))
        val state = state(requiresBillingAddress = false)

        val result = CheckoutConfigurationMerger.EmbeddedConfiguration(config).forCheckoutSession(state)

        assertThat(result.billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod).isTrue()
    }

    private fun bdcc(
        address: PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode = Automatic,
        attachDefaultsToPaymentMethod: Boolean = false,
    ) = PaymentSheet.BillingDetailsCollectionConfiguration(
        address = address,
        attachDefaultsToPaymentMethod = attachDefaultsToPaymentMethod,
    )

    private fun embeddedConfiguration(
        bdcc: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): EmbeddedPaymentElement.Configuration {
        return EmbeddedPaymentElement.Configuration.Builder("Test Merchant")
            .billingDetailsCollectionConfiguration(bdcc)
            .build()
    }

    private fun paymentSheetConfiguration(
        bdcc: PaymentSheet.BillingDetailsCollectionConfiguration,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration.Builder("Test Merchant")
            .billingDetailsCollectionConfiguration(bdcc)
            .build()
    }

    private fun state(requiresBillingAddress: Boolean): InternalState {
        return InternalState(
            key = "CheckoutBillingReconciliationTest",
            configuration = Checkout.Configuration().build(),
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(
                requiresBillingAddress = requiresBillingAddress,
            ),
            shippingName = null,
            billingName = null,
            shippingPhoneNumber = null,
            billingPhoneNumber = null,
            shippingAddress = null,
            billingAddress = null,
            flagImages = null,
        )
    }
}
