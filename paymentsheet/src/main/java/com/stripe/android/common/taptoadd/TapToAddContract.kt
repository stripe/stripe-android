package com.stripe.android.common.taptoadd

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import kotlinx.parcelize.Parcelize

internal object TapToAddContract : ActivityResultContract<TapToAddContract.Args, TapToAddResult>() {
    private const val EXTRA_ARGS: String = "extra_activity_args"

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, TapToAddActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): TapToAddResult {
        return TapToAddResult.fromIntent(intent)
    }

    @Parcelize
    data class Args(
        val mode: TapToAddMode,
        val paymentMethodMetadata: PaymentMethodMetadata,
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
