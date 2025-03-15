package com.stripe.android.googlepaylauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

@Deprecated(
    message = "This class isn't meant for public use and will be removed in a future release. " +
        "Use GooglePayPaymentMethodLauncher directly.",
)
class GooglePayPaymentMethodLauncherContract :
    ActivityResultContract<GooglePayPaymentMethodLauncherContract.Args, GooglePayPaymentMethodLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, GooglePayPaymentMethodLauncherActivity::class.java)
            .putExtras(input.toV2().toBundle())
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
    @Parcelize
    data class Args internal constructor(
        internal val config: GooglePayPaymentMethodLauncher.Config,
        internal val currencyCode: String,
        internal val amount: Int,
        internal val transactionId: String? = null,
        internal val injectionParams: InjectionParams? = null,
    ) : Parcelable {
        @JvmOverloads
        constructor(
            config: GooglePayPaymentMethodLauncher.Config,
            currencyCode: String,
            amount: Int,
            transactionId: String? = null,
        ) : this(config, currencyCode, amount, transactionId, null)

        internal fun toBundle() = bundleOf(EXTRA_ARGS to this)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }

        @Parcelize
        internal data class InjectionParams(
            @InjectorKey val injectorKey: String,
            val productUsage: Set<String>,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?
        ) : Parcelable
    }

    internal companion object {
        internal const val EXTRA_RESULT = "extra_result"
    }
}

private fun GooglePayPaymentMethodLauncherContract.Args.toV2(): GooglePayPaymentMethodLauncherContractV2.Args {
    return GooglePayPaymentMethodLauncherContractV2.Args(
        config = config,
//        currencyCode = currencyCode,
//        amount = amount.toLong(),
//        transactionId = transactionId,
        cardBrandFilter = DefaultCardBrandFilter,
        dataRequest = ""
    )
}
