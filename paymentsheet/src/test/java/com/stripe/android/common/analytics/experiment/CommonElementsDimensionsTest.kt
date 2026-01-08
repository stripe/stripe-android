package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import org.junit.Test

class CommonElementsDimensionsTest {

    @Test
    fun `displayed_payment_method_types dimension has correct value`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("displayed_payment_method_types", "card")
    }

    @Test
    fun `dimensions are correct when google pay is not available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                isGooglePayReady = false,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("displayed_payment_method_types_including_wallets", "card")
    }

    @Test
    fun `dimensions are correct when google pay is available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                isGooglePayReady = true,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("displayed_payment_method_types_including_wallets", "card,google_pay")
    }

    @Test
    fun `dimensions are correct when link is available`() {
        val validLinkState = LinkState(
            loginState = LoginState.NeedsVerification,
            configuration = createLinkConfiguration(),
            signupMode = null
        )
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                linkState = validLinkState,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("displayed_payment_method_types_including_wallets", "card,link")
    }

    @Test
    fun `dimensions are correct when link is not available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                linkState = null,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("displayed_payment_method_types_including_wallets", "card")
    }

    @Test
    fun `dimensions includes in app elements correctly`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            ),
            EventReporter.Mode.Complete,
        )

        assertThat(dimensions).containsEntry("in_app_elements_integration_type", "complete")
    }
}
