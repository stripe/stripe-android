package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class EmbeddedActivityArgs(
    val selectedPaymentMethodCode: String,
    val paymentMethodMetadata: PaymentMethodMetadata,
    val hasSavedPaymentMethods: Boolean,
    val configuration: EmbeddedPaymentElement.Configuration,
    val paymentElementCallbackIdentifier: String,
    val statusBarColor: Int?,
    val selection: PaymentSelection?,
    val customerState: CustomerState?,
    val promotion: PaymentMethodMessagePromotion?,
    val launchMode: EmbeddedLaunchMode,
) : Parcelable {
    companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"

        fun fromIntent(intent: Intent): EmbeddedActivityArgs? {
            return intent.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_ARGS, EmbeddedActivityArgs::class.java)
            }
        }
    }
}
