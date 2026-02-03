package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface TapToAddResult : Parcelable {

    @Parcelize
    data class Complete(
        val intent: StripeIntent,
    ) : TapToAddResult

    @Parcelize
    data class Canceled(
        val paymentMethod: PaymentMethod? = null,
    ) : TapToAddResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: TapToAddResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): TapToAddResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, TapToAddResult::class.java)
            }

            return result ?: Canceled()
        }
    }
}

internal object TapToAddContract : ActivityResultContract<TapToAddContract.Args, TapToAddResult>() {
    internal const val EXTRA_ARGS: String = "extra_activity_args"

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, TapToAddActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): TapToAddResult {
        return TapToAddResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val tapToAddMode: TapToAddMode,
        val paymentElementCallbackIdentifier: String,
        val productUsage: Set<String>,
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
