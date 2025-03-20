package com.stripe.android.paymentelement.embedded.manage

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
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
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, ManageResult::class.java)
            }

            return result ?: Error
        }
    }
}

internal object ManageContract : ActivityResultContract<ManageContract.Args, ManageResult>() {
    internal const val EXTRA_ARGS: String = "extra_activity_args"

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, ManageActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ManageResult {
        return ManageResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customerState: CustomerState,
        val selection: PaymentSelection?,
        val paymentElementCallbackIdentifier: String,
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
