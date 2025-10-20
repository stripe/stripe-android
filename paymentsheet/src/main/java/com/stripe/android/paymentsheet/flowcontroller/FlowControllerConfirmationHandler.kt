package com.stripe.android.paymentsheet.flowcontroller

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler.Args
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler.State
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal interface FlowControllerConfirmationHandler {
    /**
     * An activity-safe observable indicating the current confirmation state of the handler. Should only have
     * one observer.
     */
    val state: Flow<State>

    /**
     * Performs internal confirmation prerequisites that help speed up overall confirmation time
     */
    fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata)

    /**
     * Registers all internal confirmation sub-handlers onto the given lifecycle owner.
     *
     * @param activityResultCaller a caller class that can start confirmation activity flows
     * @param lifecycleOwner The owner of an observable lifecycle to attach the handlers to
     */
    fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner)

    /**
     * Starts the confirmation process.
     *
     * @param arguments required set of arguments in order to start the confirmation process
     */
    suspend fun start(arguments: Args)
}

internal class DefaultFlowControllerConfirmationHandler @Inject constructor(
    val coroutineScope: CoroutineScope,
    val confirmationHandler: ConfirmationHandler
) : FlowControllerConfirmationHandler {
    private val _state = Channel<State>()
    override val state = _state.receiveAsFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest {
                _state.send(it)
            }
        }
    }

    override fun bootstrap(paymentMethodMetadata: PaymentMethodMetadata) {
        confirmationHandler.bootstrap(paymentMethodMetadata)
    }

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )
    }

    override suspend fun start(arguments: Args) {
        confirmationHandler.start(arguments)
    }
}
