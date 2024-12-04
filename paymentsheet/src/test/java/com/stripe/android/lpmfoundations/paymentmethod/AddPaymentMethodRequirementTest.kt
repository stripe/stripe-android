package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement.InstantDebits
import com.stripe.android.lpmfoundations.paymentmethod.AddPaymentMethodRequirement.LinkCardBrand
import com.stripe.android.model.Address
import com.stripe.android.model.LinkMode
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.testing.PaymentIntentFactory
import org.junit.Test

internal class AddPaymentMethodRequirementTest {

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
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfShowingUsBankAccount() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent().copy(
                paymentMethodTypes = listOf("card", "link", "us_bank_account"),
            ),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfOnlyCardFundingSource() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent().copy(
                linkFundingSources = listOf("card"),
            ),
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testInstantDebitsReturnsFalseIfLinkCardBrand() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
            linkMode = LinkMode.LinkCardBrand,
        )

        assertThat(InstantDebits.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testLinkCardBrandReturnsTrueForCorrectLinkMode() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
            linkMode = LinkMode.LinkCardBrand,
        )

        assertThat(LinkCardBrand.isMetBy(metadata)).isTrue()
    }

    @Test
    fun testLinkCardBrandReturnsFalseIfNotCollectingEmailAndNotProvidingAttachableDefault() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
            linkMode = LinkMode.LinkCardBrand,
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                email = CollectionMode.Never,
                attachDefaultsToPaymentMethod = true,
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(),
        )

        assertThat(LinkCardBrand.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testLinkCardBrandReturnsFalseIfNotCollectingEmailAndProvidingDefaultThatsNotBeingAttached() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
            linkMode = LinkMode.LinkCardBrand,
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                email = CollectionMode.Never,
                attachDefaultsToPaymentMethod = false,
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                email = "a_totally_valid_email@email.com",
            ),
        )

        assertThat(LinkCardBrand.isMetBy(metadata)).isFalse()
    }

    @Test
    fun testLinkCardBrandReturnsTrueIfNotCollectingEmailButProvidingAttachableDefault() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = createValidInstantDebitsPaymentIntent(),
            linkMode = LinkMode.LinkCardBrand,
            billingDetailsCollectionConfiguration = BillingDetailsCollectionConfiguration(
                email = CollectionMode.Never,
                attachDefaultsToPaymentMethod = true,
            ),
            defaultBillingDetails = PaymentSheet.BillingDetails(
                email = "a_totally_valid_email@email.com",
            ),
        )

        assertThat(LinkCardBrand.isMetBy(metadata)).isTrue()
    }

    private fun createValidInstantDebitsPaymentIntent(): PaymentIntent {
        return PaymentIntentFactory.create(
            paymentMethodTypes = listOf("card", "link"),
            linkFundingSources = listOf("card", "bank_account"),
        )
    }
}
