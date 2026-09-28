package com.stripe.android.common.spms

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.state.LinkSignupModeResult
import com.stripe.android.paymentsheet.state.LinkState
import org.junit.Test

internal class DefaultLinkInlineSignupAvailabilityTest {
    @Test
    fun `availability is Unavailable when link state is null`() {
        val metadata = PaymentMethodMetadataFactory.create(linkState = null)
        val availability = DefaultLinkInlineSignupAvailability(metadata)

        assertThat(availability.availability())
            .isEqualTo(LinkInlineSignupAvailability.Result.Unavailable)
    }

    @Test
    fun `availability is Unavailable when signup mode is null`() {
        val metadata = PaymentMethodMetadataFactory.create(
            linkState = LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = LinkState.LoginState.LoggedOut,
                signupMode = null,
            ),
        )
        val availability = DefaultLinkInlineSignupAvailability(metadata)

        assertThat(availability.availability())
            .isEqualTo(LinkInlineSignupAvailability.Result.Unavailable)
    }

    @Test
    fun `availability is Unavailable when not available for saved payment methods`() {
        val linkState = LinkState(
            configuration = TestFactory.LINK_CONFIGURATION,
            loginState = LinkState.LoginState.LoggedOut,
            signupModeResult = LinkSignupModeResult.Enabled(
                mode = LinkSignupMode.InsteadOfSaveForFutureUse,
                availableForSavedPaymentMethods = false,
            ),
        )

        val metadata = PaymentMethodMetadataFactory.create(linkState = linkState)
        val availability = DefaultLinkInlineSignupAvailability(metadata)

        assertThat(availability.availability())
            .isEqualTo(LinkInlineSignupAvailability.Result.Unavailable)
    }

    @Test
    fun `availability is Available when available for saved payment methods`() {
        val linkState = LinkState(
            configuration = TestFactory.LINK_CONFIGURATION,
            loginState = LinkState.LoginState.LoggedOut,
            signupModeResult = LinkSignupModeResult.Enabled(
                mode = LinkSignupMode.InsteadOfSaveForFutureUse,
                availableForSavedPaymentMethods = true,
            ),
        )
        val metadata = PaymentMethodMetadataFactory.create(linkState = linkState)
        val availability = DefaultLinkInlineSignupAvailability(metadata)

        assertThat(availability.availability())
            .isEqualTo(
                LinkInlineSignupAvailability.Result.Available(
                    configuration = TestFactory.LINK_CONFIGURATION,
                )
            )
    }
}
