package com.stripe.android.paymentelement

/**
 * Handler interface for intercepting wallet button clicks.
 *
 * This handler is invoked when a user taps on a wallet button (e.g., Google Pay, Link, Shop Pay)
 * in the PaymentSheet or FlowController. The handler can be used to perform custom logic
 * and optionally handle the wallet button action.
 */
@WalletButtonsPreview
fun interface WalletButtonsViewClickHandler {
    /**
     * Called when a wallet button is clicked.
     *
     * @param wallet The type of wallet button that was clicked
     * @return true if the wallet button action has been handled and should not continue,
     *         false if the action has not been handled and should continue with default behavior
     */
    fun onWalletButtonClick(wallet: String): Boolean
}
