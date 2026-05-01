package com.stripe.android.payments.samsungpay

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

internal class SamsungPayLauncherContract :
    ActivityResultContract<SamsungPayLauncherContract.Args, SamsungPayLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, SamsungPayLauncherActivity::class.java)
            .putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SamsungPayLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT)
            ?: SamsungPayLauncher.Result.Failed(
                SamsungPayException(errorCode = -1, message = "No result returned")
            )
    }

    sealed class Args : Parcelable {
        abstract val clientSecret: String
        abstract val config: SamsungPayLauncher.Config

        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        data class PaymentIntentArgs(
            override val clientSecret: String,
            override val config: SamsungPayLauncher.Config,
        ) : Args()

        @Parcelize
        data class SetupIntentArgs(
            override val clientSecret: String,
            override val config: SamsungPayLauncher.Config,
            val currencyCode: String,
        ) : Args()

        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    companion object {
        const val EXTRA_ARGS = "extra_samsung_pay_launcher_args"
        const val EXTRA_RESULT = "extra_samsung_pay_launcher_result"
    }
}
