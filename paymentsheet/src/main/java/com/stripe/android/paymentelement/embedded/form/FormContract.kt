package com.stripe.android.paymentelement.embedded.form

import android.content.Intent
import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface FormResult : Parcelable {
    val customerState: CustomerState?

    @Parcelize
    data class Complete(
        val selection: PaymentSelection?,
        val hasBeenConfirmed: Boolean,
        override val customerState: CustomerState?,
    ) : FormResult

    @Parcelize
    data class Cancelled(
        override val customerState: CustomerState?
    ) : FormResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: FormResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): FormResult {
            val result = intent?.extras?.let { bundle ->
                @Suppress("DEPRECATION")
                bundle.getParcelable(EXTRA_RESULT) as? FormResult
            }

            return result ?: Cancelled(customerState = null)
        }
    }
}

internal object FormContract {
    @Parcelize
    internal data class Args(
        val selectedPaymentMethodCode: String,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val hasSavedPaymentMethods: Boolean,
        val configuration: EmbeddedPaymentElement.Configuration,
        val paymentElementCallbackIdentifier: String,
        val statusBarColor: Int?,
        val paymentSelection: PaymentSelection?,
        val customerState: CustomerState?,
    ) : Parcelable
}
