package com.stripe.android

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.CheckoutContract.Args.Companion.putCheckoutArgs
import com.stripe.android.view.CheckoutActivity
import kotlinx.android.parcel.Parcelize

internal class CheckoutContract : ActivityResultContract<CheckoutContract.Args, Checkout.CompletionStatus>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, CheckoutActivity::class.java)
            .putCheckoutArgs(input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Checkout.CompletionStatus {
        // TODO: use a real result
        return Checkout.CompletionStatus.Succeeded
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val ephemeralKey: String,
        val customerId: String
    ) : Parcelable {

        internal companion object {
            private const val EXTRA_ARGS = "checkout_activity_args"

            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }

            internal fun Intent.putCheckoutArgs(args: Args): Intent = this.putExtra(EXTRA_ARGS, args)
        }
    }
}
