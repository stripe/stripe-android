package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal interface EmbeddedActivityLauncher {
    fun launchForm(code: String, paymentMethodMetadata: PaymentMethodMetadata?)
}

internal class DefaultEmbeddedActivityLauncher(
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder
) : EmbeddedActivityLauncher {

    private var formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(
            FormContract()
        ) { result ->
            if (result is FormResult.Complete){
                selectionHolder.set(result.selection)
            }
        }

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    formActivityLauncher.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    override fun launchForm(code: String, paymentMethodMetadata: PaymentMethodMetadata?) {
        formActivityLauncher.launch(FormContract.Args(code, paymentMethodMetadata))
    }
}
