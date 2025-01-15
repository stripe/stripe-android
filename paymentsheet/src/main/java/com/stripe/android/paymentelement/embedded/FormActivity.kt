package com.stripe.android.paymentelement.embedded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.core.os.BundleCompat
import com.stripe.android.common.ui.ElementsBottomSheetLayout
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.bottomsheet.rememberStripeBottomSheetState
import com.stripe.android.uicore.utils.fadeOut
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

class FormActivity : AppCompatActivity() {
    private val args: FormContract.Args by lazy {
        FormContract.Args.fromIntent(intent) ?: throw IllegalStateException(
            "Args required"
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StripeTheme {
                val bottomSheetState = rememberStripeBottomSheetState()
                ElementsBottomSheetLayout(
                    state = bottomSheetState,
                    onDismissed = {
                        setResult(
                            Activity.RESULT_OK,
                            FormResult.toIntent(intent, FormResult.Cancelled)
                        )
                        finish()
                    }
                ) {
                    Text(args.selectedPaymentMethodCode)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

}

internal sealed interface FormResult : Parcelable {

    @Parcelize
    data class Complete(val selection: PaymentSelection) : FormResult

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



internal class FormContract : ActivityResultContract<FormContract.Args, FormResult>() {
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
        val paymentMethodMetadata: PaymentMethodMetadata?,
    ) : Parcelable {
        companion object {
            fun fromIntent(intent: Intent): Args? {
                return intent.extras?.let { bundle ->
                    BundleCompat.getParcelable(bundle, EXTRA_ARGS, Args::class.java)
                }
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}

