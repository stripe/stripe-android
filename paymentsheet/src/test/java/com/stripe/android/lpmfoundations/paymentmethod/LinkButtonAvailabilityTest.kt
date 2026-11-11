package com.stripe.android.lpmfoundations.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.LinkSignupModeResult
import com.stripe.android.paymentsheet.state.LinkState
import org.junit.Test

internal class LinkButtonAvailabilityTest {
    @Test
    fun `automatic display is available when Link is enabled`() {
        assertThat(availability()).isEqualTo(LinkButtonAvailability.Available)
    }

    @Test
    fun `absent Link state is unavailable without inventing button hiding reasons`() {
        assertThat(availability(linkState = null)).isEqualTo(
            LinkButtonAvailability.Unavailable(emptyList())
        )
    }

    @Test
    fun `Never display reports the configuration reason`() {
        assertThat(availability(display = PaymentSheet.LinkConfiguration.Display.Never)).isEqualTo(
            LinkButtonAvailability.Unavailable(listOf(LinkButtonUnavailableReason.LinkConfiguration))
        )
    }

    @Test
    fun `reports both wallet visibility and shipping restrictions`() {
        assertThat(
            availability(
                display = PaymentSheet.LinkConfiguration.Display.WalletButtonHidden,
                requiresShippingAddress = true,
            )
        ).isEqualTo(
            LinkButtonAvailability.Unavailable(
                listOf(
                    LinkButtonUnavailableReason.WalletButtonHidden,
                    LinkButtonUnavailableReason.ShippingAddressRequired,
                )
            )
        )
    }

    private fun availability(
        linkState: LinkState? = LinkState(
            configuration = TestFactory.LINK_CONFIGURATION,
            loginState = LinkState.LoginState.LoggedOut,
            signupModeResult = LinkSignupModeResult.AlreadyRegistered,
        ),
        display: PaymentSheet.LinkConfiguration.Display = PaymentSheet.LinkConfiguration.Display.Automatic,
        requiresShippingAddress: Boolean = false,
    ): LinkButtonAvailability = PaymentMethodMetadataFactory.create(
        linkState = linkState,
        linkConfiguration = PaymentSheet.LinkConfiguration(display),
    ).linkButtonAvailability(requiresShippingAddress)
}
