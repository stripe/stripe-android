package com.stripe.android.googlepaylauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.CardBrandFilter
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GooglePayPaymentMethodLauncherContractV2 :
    ActivityResultContract<GooglePayPaymentMethodLauncherContractV2.Args, GooglePayPaymentMethodLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, GooglePayPaymentMethodLauncherActivity::class.java)
            .putExtras(input.toBundle())
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): GooglePayPaymentMethodLauncher.Result {
        return intent?.getParcelableExtra(EXTRA_RESULT)
            ?: GooglePayPaymentMethodLauncher.Result.Failed(
                IllegalArgumentException("Could not parse a valid result."),
                GooglePayPaymentMethodLauncher.INTERNAL_ERROR
            )
    }

    /**
     * Args for launching [GooglePayPaymentMethodLauncherContract] to create a [PaymentMethod].
     *
     * @param config the [GooglePayPaymentMethodLauncher.Config] for this transaction
     * @param currencyCode ISO 4217 alphabetic currency code. (e.g. "USD", "EUR")
     * @param amount if the amount of the transaction is unknown at this time, set to `0`.
     * @param transactionId a unique ID that identifies a transaction attempt. Merchants may use an
     *     existing ID or generate a specific one for Google Pay transaction attempts.
     *     This field is required when you send callbacks to the Google Transaction Events API.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Args internal constructor(
        internal val config: GooglePayPaymentMethodLauncher.Config,
        internal val currencyCode: String,
        internal val amount: Long,
        internal val label: String? = null,
        internal val transactionId: String? = null,
        internal val cardBrandFilter: CardBrandFilter
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
    }
}
