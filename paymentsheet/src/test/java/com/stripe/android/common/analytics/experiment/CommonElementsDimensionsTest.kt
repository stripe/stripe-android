package com.stripe.android.common.analytics.experiment

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.LinkState.LoginState
import com.stripe.android.paymentsheet.utils.LinkTestUtils.createLinkConfiguration
import org.junit.Test

class CommonElementsDimensionsTest {

    @Test
    fun `sdk_platform dimension has correct value`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create()
        )

        assertThat(dimensions).containsEntry("sdk_platform", "android")
    }

    @Test
    fun `amount dimensions have correct values`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )
        )

        assertThat(dimensions).containsEntry("amount", "1099")
        assertThat(dimensions).containsEntry("currency", "usd")
    }

    @Test
    fun `payment_method_types dimension has correct value`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
            )
        )

        assertThat(dimensions).containsEntry("payment_method_types", "card")
    }

    @Test
    fun `dimensions are correct when google pay is not available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                isGooglePayReady = false,
            )
        )

        assertThat(dimensions).containsEntry("is_google_pay_available", "false")
        assertThat(dimensions).containsEntry("payment_method_types_including_wallets", "card")
    }

    @Test
    fun `dimensions are correct when google pay is available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                isGooglePayReady = true,
            )
        )

        assertThat(dimensions).containsEntry("is_google_pay_available", "true")
        assertThat(dimensions).containsEntry("payment_method_types_including_wallets", "card,google_pay")
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
            )
        )

        assertThat(dimensions).containsEntry("link_displayed", "true")
        assertThat(dimensions).containsEntry("payment_method_types_including_wallets", "card,link")
    }

    @Test
    fun `dimensions are correct when link is not available`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION,
                linkState = null,
            )
        )

        assertThat(dimensions).containsEntry("link_displayed", "false")
        assertThat(dimensions).containsEntry("payment_method_types_including_wallets", "card")
    }

    @Test
    fun `sdk version dimension has correct value`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create()
        )

        assertThat(dimensions).containsEntry("mobile_sdk_version", StripeSdkVersion.VERSION_NAME)
    }

    @Test
    fun `live mode dimension has correct value`() {
        val dimensions = CommonElementsDimensions.getDimensions(
            PaymentMethodMetadataFactory.create(
                stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            )
        )

        assertThat(dimensions).containsEntry("livemode", "false")
    }
}
