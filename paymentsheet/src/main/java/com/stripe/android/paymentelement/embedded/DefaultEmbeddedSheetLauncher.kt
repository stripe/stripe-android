package com.stripe.android.paymentelement.embedded

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal interface EmbeddedSheetLauncher {
    fun launchForm(code: String, paymentMethodMetadata: PaymentMethodMetadata)
}

internal fun interface EmbeddedSheetLauncherFactory {
    fun create(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ): EmbeddedSheetLauncher
}

@AssistedFactory
internal interface DefaultEmbeddedSheetLauncherFactory : EmbeddedSheetLauncherFactory {
    override fun create(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ): DefaultEmbeddedSheetLauncher
}

internal class DefaultEmbeddedSheetLauncher @AssistedInject constructor(
    @Assisted activityResultCaller: ActivityResultCaller,
    @Assisted lifecycleOwner: LifecycleOwner,
    private val selectionHolder: EmbeddedSelectionHolder,
) : EmbeddedSheetLauncher {

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

    private val formActivityLauncher: ActivityResultLauncher<FormContract.Args> =
        activityResultCaller.registerForActivityResult(FormContract()) { result ->
            if (result is FormResult.Complete) {
                selectionHolder.set(result.selection)
            }
        }

    override fun launchForm(code: String, paymentMethodMetadata: PaymentMethodMetadata) {
        formActivityLauncher.launch(FormContract.Args(code, paymentMethodMetadata))
    }
}
