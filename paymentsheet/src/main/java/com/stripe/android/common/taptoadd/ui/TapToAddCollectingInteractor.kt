package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
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
    private val onCanceled: () -> Unit,
    private val logger: Logger,
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
                logger.debug("Tap to add collection failed with error: ${collectionState.error}")
                onFailedCollection(
                    collectionState.displayMessage ?: collectionState.error.stripeErrorMessage()
                )
            }
            is TapToAddCollectionHandler.CollectionState.Canceled -> {
                onCanceled()
            }
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        @ViewModelScope private val coroutineScope: CoroutineScope,
        private val tapToAddCollectionHandler: TapToAddCollectionHandler,
        private val paymentMethodHolder: TapToAddPaymentMethodHolder,
        private val tapToAddCollectCvcInteractorFactory: TapToAddCollectCvcInteractor.Factory,
        private val tapToAddConfirmationInteractorFactory: TapToAddConfirmationInteractor.Factory,
        private val navigator: Provider<TapToAddNavigator>,
        @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    ) : TapToAddCollectingInteractor.Factory {
        override fun create(): TapToAddCollectingInteractor {
            return DefaultTapToAddCollectingInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                coroutineScope = coroutineScope,
                tapToAddCollectionHandler = tapToAddCollectionHandler,
                onCollected = { paymentMethod ->
                    paymentMethodHolder.setPaymentMethod(paymentMethod)

                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = if (requiresTapToAddCvcCollection(paymentMethodMetadata, paymentMethod)) {
                                TapToAddNavigator.Screen.CollectCvc(
                                    interactor = tapToAddCollectCvcInteractorFactory.create(paymentMethod)
                                )
                            } else {
                                TapToAddNavigator.Screen.Confirmation(
                                    interactor = tapToAddConfirmationInteractorFactory.create(
                                        paymentMethod = paymentMethod,
                                        paymentMethodOptionsParams = null,
                                    ),
                                )
                            }
                        )
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
                },
                onCanceled = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.Close,
                    )
                },
                logger = Logger.getInstance(enableLogging = enableLogging),
            )
        }
    }
}
