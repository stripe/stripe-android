package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal object AddressElementActivityContract {
    internal object Standalone : ActivityResultContract<Args.Standalone, AddressLauncherResult>() {
        override fun createIntent(context: Context, input: Args.Standalone): Intent {
            return createActivityIntent(context, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): AddressLauncherResult {
            return when (val result = parseActivityResult(intent)) {
                Result.Canceled,
                is Result.CheckoutShippingSucceeded -> AddressLauncherResult.Canceled()
                is Result.StandaloneSucceeded -> AddressLauncherResult.Succeeded(result.address)
            }
        }
    }

    internal object CheckoutShipping :
        ActivityResultContract<Args.CheckoutShipping, CheckoutShippingResult>() {
        override fun createIntent(context: Context, input: Args.CheckoutShipping): Intent {
            return createActivityIntent(context, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): CheckoutShippingResult {
            return when (val result = parseActivityResult(intent)) {
                Result.Canceled -> Result.Canceled
                is Result.CheckoutShippingSucceeded -> result
                is Result.StandaloneSucceeded -> Result.Canceled
            }
        }
    }

    private fun createActivityIntent(context: Context, input: Args): Intent {
        return Intent(context, AddressElementActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    private fun parseActivityResult(intent: Intent?): Result {
        val result = intent?.extras?.let { extras ->
            BundleCompat.getParcelable(extras, EXTRA_RESULT, Result::class.java)
        }
        return result ?: Result.Canceled
    }

    /**
     * Arguments for launching [AddressElementActivity] to collect an address.
     *
     * @param publishableKey the Stripe publishable key
     * @param config the paymentsheet configuration passed from the merchant
     */
    sealed class Args : ActivityStarter.Args {
        internal abstract val publishableKey: String
        internal abstract val config: AddressLauncher.Configuration?

        @Parcelize
        data class Standalone internal constructor(
            override val publishableKey: String,
            override val config: AddressLauncher.Configuration?,
        ) : Args()

        @Parcelize
        data class CheckoutShipping internal constructor(
            override val publishableKey: String,
            override val config: AddressLauncher.Configuration?,
        ) : Args()

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { extras ->
                    BundleCompat.getParcelable(extras, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }

    internal sealed interface StandaloneResult : Result

    internal sealed interface CheckoutShippingResult : Result

    internal sealed interface Result : ActivityStarter.Result, Parcelable {
        val resultCode: Int

        override fun toBundle() = bundleOf(EXTRA_RESULT to this)

        @Parcelize
        data object Canceled : StandaloneResult, CheckoutShippingResult {
            override val resultCode: Int
                get() = Activity.RESULT_CANCELED
        }

        @Parcelize
        data class StandaloneSucceeded(
            val address: AddressDetails,
        ) : StandaloneResult {
            override val resultCode: Int
                get() = Activity.RESULT_OK
        }

        @Parcelize
        data class CheckoutShippingSucceeded(
            val address: AddressDetails,
        ) : CheckoutShippingResult {
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
