package com.stripe.android.paymentelement.embedded

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.BundleCompat
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedActivityResult : Parcelable {

    @Parcelize
    data class Complete(
        val selection: PaymentSelection?,
        val hasBeenConfirmed: Boolean,
        val customerState: CustomerState?,
        val shouldInvokeSelectionCallback: Boolean,
    ) : EmbeddedActivityResult

    @Parcelize
    data class Cancelled(
        val customerState: CustomerState?,
    ) : EmbeddedActivityResult

    @Parcelize
    data object Error : EmbeddedActivityResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: EmbeddedActivityResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): EmbeddedActivityResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, EmbeddedActivityResult::class.java)
            }
            return result ?: Error
        }
    }
}
