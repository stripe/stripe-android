package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EmbeddedActivityLauncher {
    fun launchForm(args: FormContract.Args, onSelected: (paymentSelection: PaymentSelection) -> Unit)
}

internal class DefaultEmbeddedActivityLauncher(
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner,
) : EmbeddedActivityLauncher {

    private var formActivityLauncher: ActivityResultLauncher<FormContract.Args>? = null

    init {
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    formActivityLauncher?.unregister()
                    super.onDestroy(owner)
                }
            }
        )
    }

    override fun launchForm(args: FormContract.Args, onSelected: (paymentSelection: PaymentSelection) -> Unit) {
        if (formActivityLauncher == null ) {
            formActivityLauncher = activityResultCaller.registerForActivityResult(
                FormContract()
            ) { result ->
                if (result is FormResult.Complete) onSelected(result.selection)
            }
        }
        formActivityLauncher?.launch(args)
    }
}
