package com.stripe.android.paymentsheet.addresselement

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class AddressElementActivityContract :
    ActivityResultContract<AddressElementActivityContract.Args, AddressLauncherResult>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val statusBarColor = (context as? Activity)?.window?.statusBarColor
        return Intent(context, AddressElementActivity::class.java)
            .putExtra(EXTRA_ARGS, input.copy(statusBarColor = statusBarColor))
    }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        intent?.getParcelableExtra<Result>(EXTRA_RESULT)?.addressOptionsResult
            ?: AddressLauncherResult.Canceled

    /**
     * Arguments for launching [AddressElementActivity] to collect an address.
     *
     * @param publishableKey the Stripe publishable key
     * @param config the paymentsheet configuration passed from the merchant
     * @param injectorKey Parameter needed to perform dependency injection.
     *                        If default, a new graph is created
     */
    @Parcelize
    data class Args internal constructor(
        internal val publishableKey: String,
        internal val config: AddressLauncher.Configuration?,
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY,
        @ColorInt internal val statusBarColor: Int? = null
    ) : ActivityStarter.Args {

        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    @Parcelize
    data class Result(
        val addressOptionsResult: AddressLauncherResult
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
