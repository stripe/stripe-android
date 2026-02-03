package com.stripe.android.common.taptoadd

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface TapToAddFlowManager {
    val screen: StateFlow<TapToAddScreen>
    val result: Flow<TapToAddResult>

    fun action(action: Action)

    sealed interface Action {
        data object Close : Action
    }
}

@Singleton
internal class DefaultTapToAddFlowManager @Inject constructor(
    @ViewModelScope private val coroutineScope: CoroutineScope,
    @IOContext workContext: CoroutineContext,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val tapToAddMode: TapToAddMode,
    tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val confirmationHandler: ConfirmationHandler,
    private val tapToAddPaymentMethodHolder: TapToAddPaymentMethodHolder,
) : TapToAddFlowManager {
    private val _screen = MutableStateFlow(
        tapToAddPaymentMethodHolder.collectedPaymentMethod?.let {
            TapToAddScreen.Confirmation(
                interactor = createConfirmationInteractor(
                    collectedPaymentMethod = it,
                )
            )
        } ?: TapToAddScreen.Collecting
    )
    override val screen: StateFlow<TapToAddScreen> = _screen.asStateFlow()

    private val _result = Channel<TapToAddResult>()
    override val result: Flow<TapToAddResult> = _result.receiveAsFlow()

    init {
        if (tapToAddPaymentMethodHolder.collectedPaymentMethod == null) {
            coroutineScope.launch(workContext) {
                when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
                    is TapToAddCollectionHandler.CollectionState.Collected -> {
                        _screen.value = TapToAddScreen.Collected(
                            brand = collectionState.paymentMethod.card?.brand ?: CardBrand.Unknown,
                            last4 = collectionState.paymentMethod.card?.last4,
                        )

                        delay(timeMillis = CONFIRMATION_SCREEN_TRANSITION_DELAY)

                        _screen.value = TapToAddScreen.Confirmation(
                            interactor = createConfirmationInteractor(
                                collectedPaymentMethod = collectionState.paymentMethod,
                            )
                        )
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

    override fun action(action: TapToAddFlowManager.Action) {
        when (action) {
            is TapToAddFlowManager.Action.Close -> {
                coroutineScope.launch {
                    _result.send(
                        TapToAddResult.Canceled(tapToAddPaymentMethodHolder.collectedPaymentMethod)
                    )
                }
            }
        }
    }

    private fun createConfirmationInteractor(
        collectedPaymentMethod: PaymentMethod
    ): TapToAddConfirmationInteractor {
        return DefaultTapToAddConfirmationInteractor(
            coroutineScope = coroutineScope,
            confirmationHandler = confirmationHandler,
            customPrimaryButtonLabel = null,
            mode = tapToAddMode,
            collectedPaymentMethod = collectedPaymentMethod,
            paymentMethodMetadata = paymentMethodMetadata,
            onConfirmed = {
                coroutineScope.launch {
                    _result.send(TapToAddResult.Complete(it))
                }
            },
        )
    }

    private companion object {
        const val CONFIRMATION_SCREEN_TRANSITION_DELAY = 3000L
    }
}