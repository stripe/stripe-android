package com.stripe.android.paymentelement.embedded.manage

import android.content.Intent
import android.os.Parcelable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface ManageResult : Parcelable {

    @Parcelize
    data class Complete(
        val customerState: CustomerState,
        val selection: PaymentSelection?,
        val shouldInvokeSelectionCallback: Boolean
    ) : ManageResult

    @Parcelize
    object Error : ManageResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: ManageResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): ManageResult {
            val result = intent?.extras?.let { bundle ->
                @Suppress("DEPRECATION")
                bundle.getParcelable(EXTRA_RESULT) as? ManageResult
            }

            return result ?: Error
        }
    }
}

internal object ManageContract {
    @Parcelize
    internal data class Args(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customerState: CustomerState,
        val selection: PaymentSelection?,
        val paymentElementCallbackIdentifier: String,
    ) : Parcelable
}
