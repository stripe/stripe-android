package com.stripe.android.payments.samsungpay

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SamsungPayLauncherContract :
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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        val clientSecret: String,
        val config: Config,
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let {
                    BundleCompat.getParcelable(it, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }
}
