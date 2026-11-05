package com.stripe.android.paymentelement.embedded

import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethodCode
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedLaunchMode : Parcelable {
    @Parcelize
    data class Form(
        val selectedPaymentMethodCode: PaymentMethodCode,
    ) : EmbeddedLaunchMode

    @Parcelize
    data object Manage : EmbeddedLaunchMode

    /**
     * @param flowController non-null when FlowController presents the sheet, which keeps FlowController's payment
     * option behavior instead of Embedded's.
     */
    @Parcelize
    data class PaymentOptions(
        val flowController: FlowControllerOptions?,
    ) : EmbeddedLaunchMode

    @Parcelize
    data class FlowControllerOptions(
        val visibleWallets: List<WalletType>,
        val googlePlacesApiKey: String?,
    ) : Parcelable
}

internal val EmbeddedLaunchMode.flowControllerOptions: EmbeddedLaunchMode.FlowControllerOptions?
    get() = (this as? EmbeddedLaunchMode.PaymentOptions)?.flowController

internal val EmbeddedLaunchMode.isFlowController: Boolean
    get() = flowControllerOptions != null
