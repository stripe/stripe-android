package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal interface EmbeddedActivityLauncher {
    val formLauncher: ((code: String, paymentMethodMetadata: PaymentMethodMetadata?) -> Unit)
}

internal class DefaultEmbeddedActivityLauncher(
    activityResultCaller: ActivityResultCaller,
    private val selectionHolder: EmbeddedSelectionHolder
) : EmbeddedActivityLauncher {

    private var formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(FormContract()) { result ->
            if (result is FormResult.Complete) {
                selectionHolder.set(result.selection)
            }
        }

    override val formLauncher: ((code: String, paymentMethodMetadata: PaymentMethodMetadata?) -> Unit) =
        { code, metadata ->
            formActivityLauncher.launch(FormContract.Args(code, metadata))
        }
}
