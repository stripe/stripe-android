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
import com.stripe.android.uicore.utils.combineAsStateFlow
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
    private val sheetActivityStateHolder: SheetActivityStateHolder,
) {
    // Keep wallet placement stable while saved payment methods are added or removed.
    val canShowDirectForm: Boolean = paymentMethodMetadata.supportedPaymentMethodTypes().size == 1 &&
        customerStateHolder.paymentMethods.value.isEmpty()

    val walletsState: StateFlow<WalletsState?> = combineAsStateFlow(
        sheetActivityStateHolder.state,
        linkAccountHolder.linkAccountInfo,
        selectionHolder.selection,
    ) { sheetState, linkAccountInfo, currentSelection ->
        val linkAccount = linkAccountInfo.account
        val hasLinkWithSelectedPayment = currentSelection is PaymentSelection.Link &&
            currentSelection.selectedPayment != null
        WalletsState.create(
            isLinkAvailable = paymentMethodMetadata.shouldShowLinkButton,
            linkEmail = linkAccount?.email,
            isGooglePayReady = paymentMethodMetadata.isGooglePayReady,
            apiConfiguration = paymentMethodMetadata.apiConfiguration,
            buttonsEnabled = !sheetState.isProcessing,
            paymentMethodTypes = paymentMethodMetadata.supportedPaymentMethodTypes(),
            googlePayLauncherConfig = null,
            googlePayButtonType = GooglePayButtonType.Pay,
            onGooglePayPressed = {
                onWalletPressed(PaymentSelection.GooglePay)
            },
            onLinkPressed = {
                val linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount)
                onWalletPressed(PaymentSelection.Link(linkBrand))
            },
            isSetupIntent = paymentMethodMetadata.stripeIntent is SetupIntent,
            walletsAllowedInHeader = if (canShowDirectForm) {
                WalletType.entries
            } else {
                listOf(WalletType.Link)
            },
            paymentDetails = linkAccount?.displayablePaymentDetails,
            enableDefaultValues = paymentMethodMetadata.linkState?.configuration
                ?.enableDisplayableDefaultValuesInEce == true && hasLinkWithSelectedPayment.not(),
            cardBrandFilter = paymentMethodMetadata.cardBrandFilter,
            cardFundingFilter = paymentMethodMetadata.cardFundingFilter,
            linkBrand = paymentMethodMetadata.effectiveLinkBrand(linkAccount),
        )
    }

    private fun onWalletPressed(selection: PaymentSelection) {
        if (sheetActivityStateHolder.state.value.isProcessing) return
        selectionHolder.setSelection(selection)
        continueCoordinator.onContinue()
    }
}
