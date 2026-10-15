package com.stripe.android.elements

import android.os.Parcelable
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.parcelize.Parcelize

/** The single loaded session and selection used by every element in an ElementsController. */
@Parcelize
internal data class ElementsControllerState(
    val configurationId: String,
    val configuration: ElementsController.Configuration.State,
    val paymentElementState: PaymentElementLoader.State,
    val walletState: ElementsWalletState,
) : Parcelable {
    val paymentSelection: PaymentSelection?
        get() = paymentElementState.paymentSelection

    val paymentMethodMetadata
        get() = paymentElementState.paymentMethodMetadata

    val commonConfiguration
        get() = paymentElementState.config
}

/** Order display data shared by wallet adapters; independent of a wallet's transport objects. */
@Parcelize
internal data class ElementsWalletState(
    val amount: Long,
    val lineItems: List<ElementsController.ExpressCheckoutElement.LineItem>,
) : Parcelable
