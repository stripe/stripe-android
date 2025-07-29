package com.stripe.android.paymentsheet.addresselement

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.elements.address.AddressLauncher
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object AddressElementActivityContract :
    ActivityResultContract<AddressElementActivityContract.Args, AddressLauncher.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, AddressElementActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): AddressLauncher.Result =
        intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.addressOptionsResult
            ?: AddressLauncher.Result.Canceled

    /**
     * Arguments for launching [AddressElementActivity] to collect an address.
     *
     * @param publishableKey the Stripe publishable key
     * @param config the paymentsheet configuration passed from the merchant
     */
    @Parcelize
    data class Args internal constructor(
        internal val publishableKey: String,
        internal val config: AddressLauncher.Configuration?,
    ) : ActivityStarter.Args {

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    data class Result(
        val addressOptionsResult: AddressLauncher.Result
    ) : ActivityStarter.Result {
        override fun toBundle() = bundleOf(EXTRA_RESULT to this)
    }

    const val EXTRA_ARGS =
        "com.stripe.android.paymentsheet.addresselement" +
            ".AddressElementActivityContract.extra_args"
    const val EXTRA_RESULT =
        "com.stripe.android.paymentsheet.addresselement" +
            ".AddressElementActivityContract.extra_result"
}
