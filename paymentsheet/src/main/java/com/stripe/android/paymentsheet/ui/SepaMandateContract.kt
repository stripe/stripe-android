package com.stripe.android.paymentsheet.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class SepaMandateContract :
    ActivityResultContract<SepaMandateContract.Args, SepaMandateResult>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, SepaMandateActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): SepaMandateResult {
        @Suppress("DEPRECATION")
        return intent?.getParcelableExtra(EXTRA_RESULT) ?: SepaMandateResult.Canceled
    }

    @Parcelize
    internal data class Args(
        val merchantName: String,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
        internal const val EXTRA_RESULT: String = "extra_activity_result"
    }
}
