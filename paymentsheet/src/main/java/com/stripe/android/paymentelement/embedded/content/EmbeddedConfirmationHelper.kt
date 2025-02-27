package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface EmbeddedConfirmationHelper {
    fun confirm()
}

@ExperimentalEmbeddedPaymentElementApi
@EmbeddedPaymentElementScope
internal class DefaultEmbeddedConfirmationHelper @Inject constructor(
    private val confirmationMediator: EmbeddedConfirmationMediator,
    private val resultCallback: EmbeddedPaymentElement.ResultCallback,
    activityResultCaller: ActivityResultCaller,
    lifecycleOwner: LifecycleOwner,
) : EmbeddedConfirmationHelper {
    init {
        confirmationMediator.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner
        )

        lifecycleOwner.lifecycleScope.launch {
            confirmationMediator.result.collect { result ->
                resultCallback.onResult(result)
            }
        }
    }

    override fun confirm() {
        when (confirmationMediator.confirm()) {
            true -> Unit
            false -> {
                resultCallback.onResult(
                    EmbeddedPaymentElement.Result.Failed(IllegalStateException("Not in a state that's confirmable."))
                )
            }
        }
    }
}
