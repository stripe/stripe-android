package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class AddressElementActivityContract :
    ActivityResultContract<AddressElementActivityContract.Args, AddressElementResult>() {

    override fun createIntent(context: Context, input: Args) =
        Intent(context, AddressElementActivity::class.java)
            .putExtra(EXTRA_ARGS, input)

    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.addressOptionsResult
            ?: AddressElementResult.Canceled

    /**
     * Arguments for launching [AddressElementActivity] to collect an address.
     *
     * @param stripeIntent The Stripe Intent that is being processed
     * @param config the paymentsheet configuration passed from the merchant
     * @param injectionParams Parameters needed to perform dependency injection.
     *                        If null, a new dependency graph will be created.
     */
    @Parcelize
    data class Args internal constructor(
        internal val stripeIntent: StripeIntent,
        internal val config: PaymentSheet.Configuration?,
        internal val injectionParams: InjectionParams? = null
    ) : ActivityStarter.Args {

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }

        @Parcelize
        internal data class InjectionParams(
            @InjectorKey val injectorKey: String,
            val productUsage: Set<String>,
            val enableLogging: Boolean
        ) : Parcelable
    }

    @Parcelize
    data class Result(
        val addressOptionsResult: AddressElementResult
    ) : ActivityStarter.Result {
        override fun toBundle() = bundleOf(EXTRA_RESULT to this)
    }

    companion object {
        const val EXTRA_ARGS =
            "com.stripe.android.paymentsheet.addresselement" +
                ".AddressElementActivityContract.extra_args"
        const val EXTRA_RESULT =
            "com.stripe.android.paymentsheet.addresselement" +
                ".AddressElementActivityContract.extra_result"
    }
}
