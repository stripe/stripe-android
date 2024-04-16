package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement.InstantDebits
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Rule
import org.junit.Test

internal class AddPaymentMethodRequirementTest {

    @get:Rule
    val instantDebitsFeatureRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.instantDebits,
        isEnabled = false,
    )

    @Test
    fun testUnsupportedReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.Unsupported.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsTrueForPaymentIntents() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsFalseForPaymentIntentsWithSetupFutureUsage() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                setupFutureUsage = StripeIntent.Usage.OnSession
            )
        )
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testUnsupportedForSetupReturnsReturnsFalseForSetupIntents() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
        assertThat(AddPaymentMethodRequirement.UnsupportedForSetup.isMetBy(metadata)).isFalse()
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
        assertThat(AddPaymentMethodRequirement.ShippingAddress.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testShippingAddressReturnsTrueWhenMetadataAllowsPaymentMethodsRequiringShippingAddress() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                shipping = null
            ),
            allowsPaymentMethodsRequiringShippingAddress = true,
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.isMetBy(metadata)).isTrue()
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
        assertThat(AddPaymentMethodRequirement.ShippingAddress.isMetBy(metadata)).isFalse()
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
        assertThat(AddPaymentMethodRequirement.ShippingAddress.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testShippingAddressReturnsFalseWithSetupIntent() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD
        )
        assertThat(AddPaymentMethodRequirement.ShippingAddress.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testMerchantSupportsDelayedPaymentMethodsReturnsTrue() {
        val metadata = PaymentMethodMetadataFactory.create(allowsDelayedPaymentMethods = true)
        assertThat(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.isMetBy(metadata))
            .isTrue()
    }

    @Test
    fun testMerchantSupportsDelayedPaymentMethodsReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create(allowsDelayedPaymentMethods = false)
        assertThat(AddPaymentMethodRequirement.MerchantSupportsDelayedPaymentMethods.isMetBy(metadata))
            .isFalse()
    }

    @Test
    fun testFinancialConnectionsSdkReturnsTrue() {
        val metadata = PaymentMethodMetadataFactory.create(financialConnectionsAvailable = true)
        assertThat(AddPaymentMethodRequirement.FinancialConnectionsSdk.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testFinancialConnectionsSdkReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create(financialConnectionsAvailable = false)
        assertThat(AddPaymentMethodRequirement.FinancialConnectionsSdk.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsTrueWithDeferredFlow() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(clientSecret = null)
        )
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsTrueWithValidVerificationMethod() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodOptionsJsonString = """{"us_bank_account":{"verification_method":"automatic"}}"""
            )
        )
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testValidUsBankVerificationMethodReturnsFalse() {
        val metadata = PaymentMethodMetadataFactory.create()
        assertThat(AddPaymentMethodRequirement.ValidUsBankVerificationMethod.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsTrue() {
        instantDebitsFeatureRule.setEnabled(true)

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfFeatureNotEnabled() {
        instantDebitsFeatureRule.setEnabled(false)

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfDeferredIntent() {
        instantDebitsFeatureRule.setEnabled(true)

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent().copy(
                clientSecret = null,
            ),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfShowingUsBankAccount() {
        instantDebitsFeatureRule.setEnabled(true)

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent().copy(
                paymentMethodTypes = listOf("card", "link", "us_bank_account"),
            ),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfOnlyCardFundingSource() {
        instantDebitsFeatureRule.setEnabled(true)

        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent().copy(
                linkFundingSources = listOf("card"),
            ),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    private fun createValidInstantDebitsPaymentIntent(): PaymentIntent {
        return PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "link"),
            linkFundingSources = listOf("card", "bank_account"),
        )
    }
}
