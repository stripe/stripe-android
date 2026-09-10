package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.GooglePayButtonType
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PaymentOptionsWalletsInteractor @Inject constructor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    customerStateHolder: CustomerStateHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    linkAccountHolder: LinkAccountHolder,
    private val continueCoordinator: SheetActivityContinueCoordinator,
) {
    val showsDirectForm: Boolean = paymentMethodMetadata.supportedPaymentMethodTypes().size == 1 &&
        customerStateHolder.paymentMethods.value.isEmpty()

    val walletsState: StateFlow<WalletsState?> = linkAccountHolder.linkAccountInfo.mapAsStateFlow { linkAccountInfo ->
        WalletsState.create(
            isLinkAvailable = paymentMethodMetadata.shouldShowLinkButton,
            linkEmail = null,
            isGooglePayReady = paymentMethodMetadata.isGooglePayReady,
            buttonsEnabled = true,
            paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
            googlePayLauncherConfig = null,
            googlePayButtonType = GooglePayButtonType.Pay,
            onGooglePayPressed = {
                onWalletPressed(PaymentSelection.GooglePay)
            },
            onLinkPressed = {
                val linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccountInfo.account)
                onWalletPressed(PaymentSelection.Link(linkBrand))
            },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent,
            walletsAllowedInHeader = if (showsDirectForm) {
                WalletType.entries
            } else {
                listOf(WalletType.Link)
            },
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccountInfo.account),
        )
    }

    private fun onWalletPressed(selection: PaymentSelection) {
        selectionHolder.setSelection(selection)
        continueCoordinator.onContinue()
    }
}
