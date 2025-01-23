package com.stripe.android.paymentelement.embedded

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

internal fun interface EmbeddedWalletsHelper {
    fun walletsState(paymentMethodMetadata: PaymentMethodMetadata): StateFlow<WalletsState?>
}

internal class DefaultEmbeddedWalletsHelper @Inject constructor(
    private val linkHandler: LinkHandler,
) : EmbeddedWalletsHelper {
    override fun walletsState(paymentMethodMetadata: PaymentMethodMetadata): StateFlow<WalletsState?> {
        linkHandler.setupLink(paymentMethodMetadata.linkState)

        return combineAsStateFlow(
            linkHandler.isLinkEnabled,
            linkHandler.linkConfigurationCoordinator.emailFlow,
        ) { isLinkAvailable, linkEmail ->
            WalletsState.create(
                isLinkAvailable = isLinkAvailable,
                linkEmail = linkEmail,
                isGooglePayReady = paymentMethodMetadata.isGooglePayReady == true,
                buttonsEnabled = true,
                paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
                googlePayLauncherConfig = null, // This isn't used for embedded.
                googlePayButtonType = GooglePayButtonType.Pay, // The actual google pay button isn't shown for embedded.
                onGooglePayPressed = { throw IllegalStateException("Not possible.") },
                onLinkPressed = { throw IllegalStateException("Not possible.") },
                isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent
            )
        }
    }
}
