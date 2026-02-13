package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal interface TapToAddCollectingInteractor {
    interface Factory {
        fun create(): TapToAddCollectingInteractor
    }
}

internal class DefaultTapToAddCollectingInteractor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    coroutineScope: CoroutineScope,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val onCollected: (paymentMethod: PaymentMethod) -> Unit,
    private val onFailedCollection: (message: ResolvableString) -> Unit,
) : TapToAddCollectingInteractor {
    init {
        coroutineScope.launch {
            collect()
        }
    }

    private suspend fun collect() {
        when (val collectionState = tapToAddCollectionHandler.collect(paymentMethodMetadata)) {
            is TapToAddCollectionHandler.CollectionState.Collected -> {
                onCollected(collectionState.paymentMethod)
            }
            is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                onFailedCollection(
                    collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                )
            }
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        @ViewModelScope private val coroutineScope: CoroutineScope,
        private val tapToAddCollectionHandler: TapToAddCollectionHandler,
        private val tapToAddCardAddedInteractorFactory: TapToAddCardAddedInteractor.Factory,
        private val navigator: Provider<TapToAddNavigator>,
    ) : TapToAddCollectingInteractor.Factory {
        override fun create(): TapToAddCollectingInteractor {
            return DefaultTapToAddCollectingInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                coroutineScope = coroutineScope,
                tapToAddCollectionHandler = tapToAddCollectionHandler,
                onCollected = { paymentMethod ->
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.CardAdded(
                                interactor = tapToAddCardAddedInteractorFactory.create(paymentMethod),
                            ),
                        ),
                    )
                },
                onFailedCollection = { message ->
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.Error(
                                message = message,
                            ),
                        ),
                    )
                }
            )
        }
    }
}
