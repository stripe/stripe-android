package com.stripe.android.paymentelement.embedded.form

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface FormResult : Parcelable {
    val customerState: com.stripe.android.paymentsheet.state.CustomerState?

    @Parcelize
    data class Complete(
        val selection: com.stripe.android.paymentsheet.model.PaymentSelection?,
        val hasBeenConfirmed: Boolean,
        override val customerState: com.stripe.android.paymentsheet.state.CustomerState?,
    ) : FormResult

    @Parcelize
    data class Cancelled(
        override val customerState: com.stripe.android.paymentsheet.state.CustomerState?
    ) : FormResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: FormResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): FormResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, FormResult::class.java)
            }

            return result ?: Cancelled(customerState = null)
        }
    }
}

internal object FormContract : ActivityResultContract<EmbeddedActivityArgs, FormResult>() {
    override fun createIntent(context: Context, input: EmbeddedActivityArgs): Intent {
        return Intent(context, FormActivity::class.java)
            .putExtra(EmbeddedActivityArgs.EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FormResult {
        return FormResult.fromIntent(intent)
    }
}
