package com.stripe.android.paymentelement.embedded.sheet

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface EmbeddedSheetResult : Parcelable {

    @Parcelize
    data class Complete(
        val selection: PaymentSelection?,
        val hasBeenConfirmed: Boolean,
        val customerState: CustomerState?,
        val shouldInvokeSelectionCallback: Boolean,
    ) : EmbeddedSheetResult

    @Parcelize
    data class Cancelled(
        val customerState: CustomerState?,
    ) : EmbeddedSheetResult

    @Parcelize
    data object Error : EmbeddedSheetResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: EmbeddedSheetResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): EmbeddedSheetResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, EmbeddedSheetResult::class.java)
            }
            return result ?: Error
        }
    }
}

internal object EmbeddedSheetContract : ActivityResultContract<EmbeddedSheetContract.Args, EmbeddedSheetResult>() {
    internal const val EXTRA_ARGS: String = "extra_activity_args"

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, EmbeddedSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): EmbeddedSheetResult {
        return EmbeddedSheetResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val selectedPaymentMethodCode: String,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val hasSavedPaymentMethods: Boolean,
        val configuration: EmbeddedPaymentElement.Configuration,
        val paymentElementCallbackIdentifier: String,
        val statusBarColor: Int?,
        val selection: PaymentSelection?,
        val customerState: CustomerState?,
        val promotion: PaymentMethodMessagePromotion?,
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { bundle ->
                    BundleCompat.getParcelable(bundle, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }
}
