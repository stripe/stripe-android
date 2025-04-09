package com.stripe.android.paymentelement.embedded.form

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal sealed interface FormResult : Parcelable {

    @Parcelize
    data class Complete(
        val selection: PaymentSelection?,
        val hasBeenConfirmed: Boolean,
    ) : FormResult

    @Parcelize
    object Cancelled : FormResult

    companion object {
        internal const val EXTRA_RESULT = ActivityStarter.Result.EXTRA

        fun toIntent(intent: Intent, result: FormResult): Intent {
            return intent.putExtra(EXTRA_RESULT, result)
        }

        fun fromIntent(intent: Intent?): FormResult {
            val result = intent?.extras?.let { bundle ->
                BundleCompat.getParcelable(bundle, EXTRA_RESULT, FormResult::class.java)
            }

            return result ?: Cancelled
        }
    }
}

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal object FormContract : ActivityResultContract<FormContract.Args, FormResult>() {
    internal const val EXTRA_ARGS: String = "extra_activity_args"

    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, FormActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): FormResult {
        return FormResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val selectedPaymentMethodCode: String,
        val paymentMethodMetadata: PaymentMethodMetadata,
        val hasSavedPaymentMethods: Boolean,
        val configuration: EmbeddedPaymentElement.Configuration,
        val initializationMode: PaymentElementLoader.InitializationMode,
        val paymentElementCallbackIdentifier: String,
        val statusBarColor: Int?,
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { bundle ->
                    BundleCompat.getParcelable(bundle, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }
}
