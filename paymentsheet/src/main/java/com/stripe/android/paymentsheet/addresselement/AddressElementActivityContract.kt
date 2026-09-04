package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object AddressElementActivityContract :
    ActivityResultContract<AddressElementActivityContract.Args, AddressElementActivityContract.Result>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, AddressElementActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    @Suppress("DEPRECATION")
    override fun parseResult(resultCode: Int, intent: Intent?): Result =
        intent?.getParcelableExtra<Result>(EXTRA_RESULT)
            ?: Result.Canceled

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
        internal val launchMode: LaunchMode,
    ) : ActivityStarter.Args {

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    sealed class LaunchMode : Parcelable {
        @Parcelize
        data object Standalone : LaunchMode()

        @Parcelize
        data object CheckoutShipping : LaunchMode()
    }

    @Parcelize
    sealed class Result : ActivityStarter.Result, Parcelable {
        abstract val resultCode: Int

        override fun toBundle() = bundleOf(EXTRA_RESULT to this)

        @Parcelize
        data object Canceled : Result() {
            override val resultCode: Int
                get() = Activity.RESULT_CANCELED
        }

        @Parcelize
        data class StandaloneSucceeded(
            val address: AddressDetails,
        ) : Result() {
            override val resultCode: Int
                get() = Activity.RESULT_OK
        }

        @Parcelize
        data class CheckoutShippingSucceeded(
            val address: AddressDetails,
        ) : Result() {
            override val resultCode: Int
                get() = Activity.RESULT_OK
        }
    }

    const val EXTRA_ARGS =
        "com.stripe.android.paymentsheet.addresselement" +
            ".AddressElementActivityContract.extra_args"
    const val EXTRA_RESULT =
        "com.stripe.android.paymentsheet.addresselement" +
            ".AddressElementActivityContract.extra_result"
}
