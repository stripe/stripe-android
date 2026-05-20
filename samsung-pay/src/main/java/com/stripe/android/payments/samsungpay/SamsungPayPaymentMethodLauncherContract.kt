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
class SamsungPayPaymentMethodLauncherContract :
    ActivityResultContract<SamsungPayPaymentMethodLauncherContract.Args, SamsungPayPaymentMethodLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, SamsungPayPaymentMethodLauncherActivity::class.java)
            .putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SamsungPayPaymentMethodLauncher.Result {
        return intent?.let {
            BundleCompat.getParcelable(
                it.extras ?: return@let null,
                EXTRA_RESULT,
                SamsungPayPaymentMethodLauncher.Result::class.java,
            )
        } ?: SamsungPayPaymentMethodLauncher.Result.Failed(
            error = IllegalArgumentException("Could not parse a valid result."),
            errorCode = SamsungPayPaymentMethodLauncher.INTERNAL_ERROR,
        )
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Args(
        internal val config: Config,
        internal val currencyCode: String,
        internal val amount: Long,
        internal val orderNumber: String,
    ) : Parcelable {
        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
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
