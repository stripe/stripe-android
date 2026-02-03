package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.taptoadd.TapToAddConfirmationOption
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal interface TapToAddHelper {
    val collectedPaymentMethod: StateFlow<DisplayableSavedPaymentMethod?>

    /**
     * Begins collection of payment method from the Tap to Add flow. Calling this method should show a screen that
     * indicates where to tap your card on your device.
     */
    fun startPaymentMethodCollection()

    companion object {
        fun create(
            coroutineScope: CoroutineScope,
            confirmationHandler: ConfirmationHandler,
            paymentMethodMetadata: PaymentMethodMetadata,
            tapToAddMode: TapToAddMode,
        ): TapToAddHelper? {
            return if (paymentMethodMetadata.isTapToAddSupported) {
                DefaultTapToAddHelper(
                    coroutineScope = coroutineScope,
                    paymentMethodMetadata = paymentMethodMetadata,
                    confirmationHandler = confirmationHandler,
                    tapToAddMode = tapToAddMode,
                )
            } else {
                null
            }
        }
    }
}

internal class DefaultTapToAddHelper(
    private val coroutineScope: CoroutineScope,
    private val confirmationHandler: ConfirmationHandler,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val tapToAddMode: TapToAddMode,
) : TapToAddHelper {
    private val _collectedPaymentMethod = MutableStateFlow<DisplayableSavedPaymentMethod?>(null)
    override val collectedPaymentMethod = _collectedPaymentMethod.asStateFlow()

    init {
        coroutineScope.launch {
            confirmationHandler.state.collectLatest { state ->
                if (
                    state is ConfirmationHandler.State.Complete &&
                    state.result is ConfirmationHandler.Result.Canceled
                ) {
                }
            }
        }
    }

    override fun startPaymentMethodCollection() {
        coroutineScope.launch {
            confirmationHandler.start(
                arguments = ConfirmationHandler.Args(
                    confirmationOption = TapToAddConfirmationOption(
                        mode = tapToAddMode,
                    ),
                    paymentMethodMetadata = paymentMethodMetadata,
                )
            )
        }
    }
}
