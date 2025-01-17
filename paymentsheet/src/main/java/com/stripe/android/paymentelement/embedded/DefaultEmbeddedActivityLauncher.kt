package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal interface EmbeddedActivityLauncher {
    var formLauncher: ((code: String, paymentMethodMetadata: PaymentMethodMetadata?) -> Unit)?
}

internal class DefaultEmbeddedActivityLauncher(
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder
) : EmbeddedActivityLauncher {

    private var formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(FormContract()) { result ->
            if (result is FormResult.Complete) {
                selectionHolder.set(result.selection)
            }
        }

    override var formLauncher: ((code: String, paymentMethodMetadata: PaymentMethodMetadata?) -> Unit)? =
        { code, metadata ->
            formActivityLauncher.launch(FormContract.Args(code, metadata))
        }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    formActivityLauncher.unregister()
                    formLauncher = null
                    super.onDestroy(owner)
                }
            }
        )
    }
}
