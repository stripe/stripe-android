package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object NfcScanningContract : ActivityResultContract<NfcScanningContract.Args, NfcScanningContract.Result>() {
    private const val EXTRA_ARGS = "com.stripe.android.common.nfcscan.NfcScanningContract.extra_args"
    private const val EXTRA_RESULT = "com.stripe.android.common.nfcscan.NfcScanningContract.extra_result"

    @Parcelize
    data class Args(
        val merchantName: String? = null,
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { bundle ->
                    BundleCompat.getParcelable(bundle, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, NfcScanningActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): Result =
        intent?.getParcelableExtra(EXTRA_RESULT)
            ?: Result.Canceled

    internal sealed interface Result : ActivityStarter.Result {
        @Parcelize
        data class Complete(
            val cardNumber: String,
            val expirationMonth: Int,
            val expirationYear: Int,
            val shouldSave: Boolean,
        ) : Result

        @Parcelize
        data object Canceled : Result

        @Parcelize
        data object AddManually : Result

        override fun toBundle(): Bundle {
            return bundleOf(EXTRA_RESULT to this)
        }
    }
}
