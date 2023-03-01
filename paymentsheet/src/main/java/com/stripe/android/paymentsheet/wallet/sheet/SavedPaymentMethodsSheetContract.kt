package com.stripe.android.paymentsheet.wallet.sheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class SavedPaymentMethodsSheetContract :
    ActivityResultContract<SavedPaymentMethodsSheetContract.Args, SavedPaymentMethodsSheetResult?>() {

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, SavedPaymentMethodsSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SavedPaymentMethodsSheetResult? {
        return SavedPaymentMethodsSheetResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        @ColorInt val statusBarColor: Int?,
        @InjectorKey val injectorKey: String,
        val productUsage: Set<String>,
        val paymentSheetConfig: PaymentSheet.Configuration,
        val paymentSelection: PaymentSelection? = null,
        val isGooglePayReady: Boolean = false,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}