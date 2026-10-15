package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VippsDefinitionTest {
    @Test
    fun `Vipps from the intent is available for payment with its display resources`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "vipps"),
                currency = "nok",
            ),
        )

        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card", "vipps").inOrder()
        val vipps = requireNotNull(metadata.supportedPaymentMethodForCode("vipps"))
        assertThat(vipps.displayName).isEqualTo(R.string.stripe_paymentsheet_payment_method_vipps.resolvableString)
        assertThat(vipps.iconResource).isEqualTo(R.drawable.stripe_ic_paymentsheet_pm_vipps_day)
        assertThat(vipps.iconResourceNight).isEqualTo(R.drawable.stripe_ic_paymentsheet_pm_vipps_night)
        assertThat(metadata.supportedSavedPaymentMethodTypes()).doesNotContain(PaymentMethod.Type.Vipps)
    }

    @Test
    fun `Vipps is unavailable when the intent does not include it`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card"),
            ),
        )

        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card")
        assertThat(metadata.supportedPaymentMethodForCode("vipps")).isNull()
    }

    @Test
    fun `Vipps is available for setup intents`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "vipps"),
            ),
        )

        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card", "vipps").inOrder()
        assertThat(metadata.supportedPaymentMethodForCode("vipps")?.code).isEqualTo("vipps")
    }

    @Test
    fun `Vipps is available for payments with future use`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "vipps"),
                setupFutureUsage = StripeIntent.Usage.OffSession,
            ),
        )

        assertThat(metadata.supportedPaymentMethodTypes()).containsExactly("card", "vipps").inOrder()
        assertThat(metadata.supportedPaymentMethodForCode("vipps")?.code).isEqualTo("vipps")
    }

    @Test
    fun `createFormElements returns no elements`() {
        VippsDefinition.basicEmptyFormTest()
    }

    @Test
    fun `createFormElements returns requested contact information fields`() {
        VippsDefinition.basicFormWithContactFieldsTest()
    }

    @Test
    fun `createFormElements returns all billing details fields`() {
        VippsDefinition.basicFormWithBillingInformationTest()
    }
}
