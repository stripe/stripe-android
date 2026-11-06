package com.stripe.android.paymentsheet.paymentdatacollection.upi

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.payments.PaymentFlowResult
import kotlinx.parcelize.Parcelize

internal class UpiAppChooserContract :
    ActivityResultContract<UpiAppChooserContract.Args, PaymentFlowResult.Unvalidated>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, UpiAppChooserActivity::class.java).putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val mobileAuthUrl: String?,
        val apiConfiguration: ApiConfiguration.State,
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { BundleCompat.getParcelable(it, EXTRA_ARGS, Args::class.java) }
            }
        }
    }

    private companion object {
        const val EXTRA_ARGS = "extra_upi_app_chooser_args"
    }
}
