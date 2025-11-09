package com.stripe.android.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.toDisplayableSavedPaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface TapToAddHelper {
    val collectedPaymentMethod: StateFlow<DisplayableSavedPaymentMethod?>

    fun collect()

    fun clear()

    companion object {
        fun create(
            coroutineScope: CoroutineScope,
            tapToAddCollectionHandler: TapToAddCollectionHandler,
            paymentMethodMetadata: PaymentMethodMetadata,
            providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
            onCollected: () -> Unit,
            updateProcessing: (processing: Boolean) -> Unit,
            updateError: (ResolvableString) -> Unit,
        ): TapToAddHelper? {
            return if (paymentMethodMetadata.isTapToAddSupported) {
                DefaultTapToAddHelper(
                    coroutineScope = coroutineScope,
                    paymentMethodMetadata = paymentMethodMetadata,
                    tapToAddCollectionHandler = tapToAddCollectionHandler,
                    providePaymentMethodName = providePaymentMethodName,
                    onCollected = onCollected,
                    updateProcessing = updateProcessing,
                    updateError = updateError,
                )
            } else {
                null
            }
        }
    }
}

private class DefaultTapToAddHelper(
    private val coroutineScope: CoroutineScope,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
    private val onCollected: () -> Unit,
    private val updateProcessing: (processing: Boolean) -> Unit,
    private val updateError: (ResolvableString) -> Unit,
) : TapToAddHelper {
    private val _collectedPaymentMethod = MutableStateFlow<DisplayableSavedPaymentMethod?>(null)
    override val collectedPaymentMethod = _collectedPaymentMethod.asStateFlow()

    override fun collect() {
        coroutineScope.launch {
            val customerMetadata = paymentMethodMetadata.customerMetadata ?: return@launch

            updateProcessing(true)

            val collectionState = tapToAddCollectionHandler.collect(
                customerId = customerMetadata.id,
                allowRedisplay = paymentMethodMetadata.paymentMethodSaveConsentBehavior.allowRedisplay(
                    isSetupIntent = true,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse,
                ),
            )

            when (collectionState) {
                is TapToAddCollectionHandler.CollectionState.Collected -> {
                    _collectedPaymentMethod.value = collectionState.paymentMethod.toDisplayableSavedPaymentMethod(
                        paymentMethodMetadata = paymentMethodMetadata,
                        providePaymentMethodName = providePaymentMethodName,
                        defaultPaymentMethodId = null,
                    )

                    onCollected()
                }
                is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                    updateError(
                        collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                    )
                }
            }

            updateProcessing(false)
        }
    }

    override fun clear() {
        _collectedPaymentMethod.value = null
    }
}
