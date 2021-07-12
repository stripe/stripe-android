package com.stripe.android.googlepaylauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

class GooglePayPaymentMethodLauncherContract :
    ActivityResultContract<GooglePayPaymentMethodLauncherContract.Args, GooglePayPaymentMethodLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val statusBarColor = when (context) {
            is Activity -> context.window?.statusBarColor
            else -> null
        }

        val extras = input.toBundle().apply {
            if (statusBarColor != null) {
                putInt(EXTRA_STATUS_BAR_COLOR, statusBarColor)
            }
        }

        return Intent(context, GooglePayPaymentMethodLauncherActivity::class.java)
            .putExtras(extras)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayPaymentMethodLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT)
            ?: GooglePayPaymentMethodLauncher.Result.Failed(
                IllegalArgumentException("Could not parse a valid result.")
            )
    }

    /**
     * Args for launching [GooglePayPaymentMethodLauncherContract] to create a [PaymentMethod].
     *
     * @param config the [GooglePayPaymentMethodLauncher.Config] for this transaction
     * @param currencyCode ISO 4217 alphabetic currency code. (e.g. "USD", "EUR")
     * @param amount if the amount of the transaction is unknown at this time, set to `0`.
     */
    @Parcelize
    data class Args(
        internal val config: GooglePayPaymentMethodLauncher.Config,
        internal val currencyCode: String,
        internal val amount: Int
    ) : Parcelable {

        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "extra_result"
        internal const val EXTRA_STATUS_BAR_COLOR = "extra_status_bar_color"
    }
}
