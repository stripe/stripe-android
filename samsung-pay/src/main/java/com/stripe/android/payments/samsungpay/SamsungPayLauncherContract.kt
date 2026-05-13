package com.stripe.android.payments.samsungpay

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

internal class SamsungPayLauncherContract :
    ActivityResultContract<SamsungPayLauncherContract.Args, SamsungPayResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, SamsungPayLauncherActivity::class.java)
            .putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SamsungPayResult {
        return intent?.getParcelableExtra(EXTRA_RESULT)
            ?: SamsungPayResult.Failure(
                IllegalStateException("Error while processing result from Samsung Pay.")
            )
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val config: Config,
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        const val EXTRA_RESULT = "extra_result"
    }
}
