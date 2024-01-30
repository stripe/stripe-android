package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import org.junit.Test

internal class AddPaymentMethodRequirementTest {
    @Test
    fun testUnsupportedReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.Unsupported.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsTrueForPaymentIntents() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.meetsRequirements(metadata)).isTrue()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsFalseForPaymentIntentsWithSetupFutureUsage() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession
            )
        )
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsFalseForSetupIntents() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testShippingAddressReturnsTrue() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                shipping = PaymentIntent.Shipping(
                    name = "Example Buyer",
                    address = Address(
                        line1 = "123 Main St",
                        country = "US",
                        postalCode = "12345"
                    )
                )
            )
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.meetsRequirements(metadata)).isTrue()
    }

    @Test
    fun testShippingAddressReturnsFalseWithNullName() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                shipping = PaymentIntent.Shipping(
                    address = Address(
                        line1 = "123 Main St",
                        country = "US",
                        postalCode = "12345"
                    )
                )
            )
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testShippingAddressReturnsFalseWithNullLine1() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                shipping = PaymentIntent.Shipping(
                    name = "Example Buyer",
                    address = Address(
                        country = "US",
                        postalCode = "12345"
                    )
                )
            )
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testShippingAddressReturnsFalseWithSetupIntent() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testMerchantSupportsDelayedPaymentMethodsReturnsTrue() {
        val metadata = PaymentMethodMetadataFactory.create(allowsDelayedPaymentMethods = true)
        assertThat(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.meetsRequirements(metadata))
            .isTrue()
    }

    @Test
    fun testMerchantSupportsDelayedPaymentMethodsReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create(allowsDelayedPaymentMethods = false)
        assertThat(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.meetsRequirements(metadata))
            .isFalse()
    }

    @Test
    fun testFinancialConnectionsSdkReturnsTrue() {
        val metadata = PaymentMethodMetadataFactory.create(financialConnectionsAvailable = true)
        assertThat(AddPaymentMethodRequirement.FinancialConnectionsSdk.meetsRequirements(metadata)).isTrue()
    }

    @Test
    fun testFinancialConnectionsSdkReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create(financialConnectionsAvailable = false)
        assertThat(AddPaymentMethodRequirement.FinancialConnectionsSdk.meetsRequirements(metadata)).isFalse()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsTrueWithDeferredFlow() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(clientSecret = null)
        )
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.meetsRequirements(metadata)).isTrue()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsTrueWithValidVerificationMethod() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}"""
            )
        )
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.meetsRequirements(metadata)).isTrue()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.meetsRequirements(metadata)).isFalse()
    }
}
