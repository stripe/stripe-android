package com.stripe.android.common.taptoadd

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal interface TapToAddHelper {
    val nextStep: SharedFlow<TapToAddNextStep>

    val hasPreviouslyAttemptedCollection: Boolean

    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    )

    /**
     * Begins collection of payment method from the Tap to Add flow. Calling this method should show a screen that
     * indicates where to tap your card on your device.
     */
    fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata)

    interface Factory {
        fun create(
            coroutineScope: CoroutineScope,
            tapToAddMode: TapToAddMode,
            updateSelection: (PaymentSelection.Saved) -> Unit,
            customerStateHolder: CustomerStateHolder,
        ): TapToAddHelper
    }
}

internal class DefaultTapToAddHelper constructor(
    private val coroutineScope: CoroutineScope,
    private val productUsage: Set<String>,
    private val paymentElementCallbackIdentifier: String,
    private val tapToAddMode: TapToAddMode,
    private val eventMode: EventReporter.Mode,
    private val savedStateHandle: SavedStateHandle,
    private val updateSelection: (PaymentSelection.Saved) -> Unit,
    private val customerStateHolder: CustomerStateHolder,
) : TapToAddHelper {
    private var collecting: Boolean
        get() = savedStateHandle.get<Boolean>(CURRENTLY_COLLECTING_WITH_TAP_TO_ADD_KEY) == true
        set(value) {
            savedStateHandle[CURRENTLY_COLLECTING_WITH_TAP_TO_ADD_KEY] = value
        }

    private var launcher: ActivityResultLauncher<TapToAddContract.Args>? = null

    private var _hasPreviouslyAttemptedCollection
        get() = savedStateHandle.get<Boolean>(PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY) == true
        set(value) {
            savedStateHandle[PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY] = value
        }

    override val hasPreviouslyAttemptedCollection: Boolean
        get() = _hasPreviouslyAttemptedCollection

    private val _nextStep = MutableSharedFlow<TapToAddNextStep>()
    override val nextStep: SharedFlow<TapToAddNextStep> = _nextStep.asSharedFlow()

    override fun register(activityResultCaller: ActivityResultCaller, lifecycleOwner: LifecycleOwner) {
        val launcher = activityResultCaller.registerForActivityResult(TapToAddContract) { result ->
            collecting = false

            coroutineScope.launch {
                mapResultToNextStep(result)?.let {
                    _nextStep.emit(it)
                }
            }
        }

        this.launcher = launcher

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    launcher.unregister()
                    this@DefaultTapToAddHelper.launcher = null
                    super.onDestroy(owner)
                }
            }
        )
    }

    private fun mapResultToNextStep(tapToAddResult: TapToAddResult): TapToAddNextStep? {
        return when (tapToAddResult) {
            is TapToAddResult.Canceled -> tapToAddResult.paymentSelection?.let { paymentSelection ->
                customerStateHolder.addPaymentMethod(paymentSelection.paymentMethod)
                updateSelection(paymentSelection)
                TapToAddNextStep.ConfirmSavedPaymentMethod(
                    paymentSelection,
                )
            }
            TapToAddResult.Complete -> TapToAddNextStep.Complete
            is TapToAddResult.Continue -> TapToAddNextStep.Continue(tapToAddResult.paymentSelection)
        }
    }

    override fun startPaymentMethodCollection(paymentMethodMetadata: PaymentMethodMetadata) {
        _hasPreviouslyAttemptedCollection = true

        if (collecting) {
            return
        }

        launcher?.run {
            collecting = true

            launch(
                input = TapToAddContract.Args(
                    mode = tapToAddMode,
                    eventMode = eventMode,
                    paymentMethodMetadata = paymentMethodMetadata,
                    paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                    productUsage = productUsage,
                )
            )
        }
    }

    class Factory @Inject constructor(
        @Named(PRODUCT_USAGE) private val productUsage: Set<String>,
        @PaymentElementCallbackIdentifier private val paymentElementCallbackIdentifier: String,
        private val savedStateHandle: SavedStateHandle,
        private val eventMode: EventReporter.Mode,
    ) : TapToAddHelper.Factory {
        override fun create(
            coroutineScope: CoroutineScope,
            tapToAddMode: TapToAddMode,
            updateSelection: (PaymentSelection.Saved) -> Unit,
            customerStateHolder: CustomerStateHolder,
        ): TapToAddHelper {
            return DefaultTapToAddHelper(
                coroutineScope = coroutineScope,
                productUsage = productUsage,
                paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                tapToAddMode = tapToAddMode,
                eventMode = eventMode,
                savedStateHandle = savedStateHandle,
                updateSelection = updateSelection,
                customerStateHolder = customerStateHolder,
            )
        }
    }

    private companion object {
        const val PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD_KEY = "PREVIOUSLY_COLLECTED_WITH_TAP_TO_ADD"
        const val CURRENTLY_COLLECTING_WITH_TAP_TO_ADD_KEY = "CURRENTLY_COLLECTING_WITH_TAP_TO_ADD"
    }
}
