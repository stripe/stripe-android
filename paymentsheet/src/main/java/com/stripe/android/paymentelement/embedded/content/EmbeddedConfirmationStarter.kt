package com.stripe.android.paymentelement.embedded.content

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EmbeddedConfirmationStarter @Inject constructor(
    private val confirmationHandler: ConfirmationHandler,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) {
    init {
        coroutineScope.launch {
            confirmationHandler.state.collect { state ->
                when (state) {
                    is ConfirmationHandler.State.Confirming,
                    is ConfirmationHandler.State.Idle -> Unit
                    is ConfirmationHandler.State.Complete -> {
                        _result.send(state.result)
                    }
                }
            }
        }
    }

    private val _result = Channel<ConfirmationHandler.Result>()
    val result = _result.receiveAsFlow()

    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )
    }

    fun start(args: ConfirmationHandler.Args) {
        confirmationHandler.start(args)
    }
}
