package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.paymentmenthod.withCardPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.SetupIntentFixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NativeLinkFormHelperFactoryTest {

    @Test
    fun `card is supported for a Link-only payment intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("link"),
            ),
        )
        assertThat(metadata.supportedPaymentMethodForCode("card")).isNull()

        val linkMetadata = metadata.withCardPaymentMethod()

        assertThat(linkMetadata.stripeIntent.paymentMethodTypes).containsExactly("link", "card").inOrder()
        assertThat(linkMetadata.supportedPaymentMethodForCode("card")).isNotNull()
    }

    @Test
    fun `card is supported for a Link-only setup intent`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("link"),
            ),
        )
        assertThat(metadata.supportedPaymentMethodForCode("card")).isNull()

        val linkMetadata = metadata.withCardPaymentMethod()

        assertThat(linkMetadata.stripeIntent.paymentMethodTypes).containsExactly("link", "card").inOrder()
        assertThat(linkMetadata.supportedPaymentMethodForCode("card")).isNotNull()
    }

    @Test
    fun `metadata is unchanged when the intent already accepts cards`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "link"),
            ),
        )

        assertThat(metadata.withCardPaymentMethod()).isSameInstanceAs(metadata)
    }
}
