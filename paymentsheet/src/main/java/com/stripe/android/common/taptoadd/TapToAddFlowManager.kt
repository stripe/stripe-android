package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface TapToAddFlowManager {
    val screen: StateFlow<TapToAddScreen>
}

@Singleton
internal class DefaultTapToAddFlowManager @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    @IOContext workContext: CoroutineContext,
    paymentMethodMetadata: PaymentMethodMetadata,
    tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val confirmationHandler: ConfirmationHandler,
    private val tapToAddPaymentMethodHolder: TapToAddPaymentMethodHolder,
) : TapToAddFlowManager {
    private val _screen = MutableStateFlow(
        if (shouldImmediatelyExecute()) {
            TapToAddScreen.Collecting
        } else {
            TapToAddScreen.Confirmation(interactor = createConfirmationInteractor())
        }
    )
    override val screen: StateFlow<TapToAddScreen> = _screen.asStateFlow()

    init {
        if (shouldImmediatelyExecute()) {
            coroutineScope.launch(workContext) {
                when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
                    is TapToAddCollectionHandler.CollectionState.Collected -> {
                        _screen.value = TapToAddScreen.Collected(
                            brand = collectionState.paymentMethod.card?.brand ?: CardBrand.Unknown,
                            last4 = collectionState.paymentMethod.card?.last4,
                        )

                        delay(timeMillis = CONFIRMATION_SCREEN_TRANSITION_DELAY)

                        _screen.value = TapToAddScreen.Confirmation(interactor = createConfirmationInteractor())
                    }
                    is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                        _screen.value = TapToAddScreen.Error(
                            message = collectionState.displayMessage
                                ?: collectionState.error.stripeErrorMessage()
                        )
                    }
                }
            }
        }
    }

    private fun shouldImmediatelyExecute(): Boolean {
        return tapToAddPaymentMethodHolder.collectedPaymentMethod == null &&
            !confirmationHandler.hasReloadedFromProcessDeath
    }

    private fun createConfirmationInteractor(): TapToAddConfirmationInteractor {
        return DefaultTapToAddConfirmationInteractor(
            coroutineScope = coroutineScope,
            confirmationHandler = confirmationHandler,
        )
    }

    private companion object {
        const val CONFIRMATION_SCREEN_TRANSITION_DELAY = 3000L
    }
}