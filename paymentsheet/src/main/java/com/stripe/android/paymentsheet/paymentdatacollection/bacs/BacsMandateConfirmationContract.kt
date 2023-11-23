package com.stripe.android.paymentsheet.paymentdatacollection.bacs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

internal class BacsMandateConfirmationContract :
    ActivityResultContract<BacsMandateConfirmationContract.Args, BacsMandateConfirmationResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, BacsMandateConfirmationActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): BacsMandateConfirmationResult {
        return BacsMandateConfirmationResult.fromIntent(intent)
    }

    @Parcelize
    data class Args(
        val email: String,
        val nameOnAccount: String,
        val sortCode: String,
        val accountNumber: String,
        val appearance: PaymentSheet.Appearance
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_ARGS, Args::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_ARGS)
                }
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
