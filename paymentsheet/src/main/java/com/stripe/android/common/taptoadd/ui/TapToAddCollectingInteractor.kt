package com.stripe.android.common.taptoadd.ui

import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.common.spms.LinkInlineSignupAvailability
import com.stripe.android.common.taptoadd.TapToAddCollectionHandler
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.EventReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

internal interface TapToAddCollectingInteractor {
    fun close()

    interface Factory {
        fun create(): TapToAddCollectingInteractor
    }
}

internal class DefaultTapToAddCollectingInteractor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    uiContext: CoroutineContext,
    ioContext: CoroutineContext,
    private val tapToAddCollectionHandler: TapToAddCollectionHandler,
    private val eventReporter: EventReporter,
    private val linkInlineSignupAvailability: LinkInlineSignupAvailability,
    private val onCollected: (paymentMethod: PaymentMethod) -> Unit,
    private val onFailedCollection: (message: ResolvableString) -> Unit,
    private val onTapToAddNotSupported: () -> Unit,
    private val onCanceled: () -> Unit,
    private val logger: Logger,
) : TapToAddCollectingInteractor {
    private val coroutineScope = CoroutineScope(uiContext + SupervisorJob())

    init {
        coroutineScope.launch {
            eventReporter.onTapToAddStarted()

            val collectionState = withContext(ioContext) {
                tapToAddCollectionHandler.collect(paymentMethodMetadata)
            }

            handleCollectionState(collectionState)
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    private fun handleCollectionState(collectionState: TapToAddCollectionHandler.CollectionState) {
        when (collectionState) {
            is TapToAddCollectionHandler.CollectionState.Collected -> {
                eventReporter.onCardAddedWithTapToAdd(
                    canCollectLinkInput =
                        linkInlineSignupAvailability.availability() is LinkInlineSignupAvailability.Result.Available
                )

                onCollected(collectionState.paymentMethod)
            }
            is TapToAddCollectionHandler.CollectionState.FailedCollection -> {
                eventReporter.onFailedToAddCardWithTapToAdd(collectionState.errorCode.value)
                logger.debug("Tap to add collection failed with error: ${collectionState.error}")
                onFailedCollection(collectionState.displayMessage ?: collectionState.error.stripeErrorMessage())
            }
            is TapToAddCollectionHandler.CollectionState.UnsupportedDevice -> {
                eventReporter.onTapToAddAttemptWithUnsupportedDevice()
                logger.debug("Tap to add collection is not supported on this device: ${collectionState.error}")
                onTapToAddNotSupported()
            }
            is TapToAddCollectionHandler.CollectionState.Canceled -> {
                eventReporter.onTapToAddCanceled(EventReporter.TapToAddCancelSource.CardCollection)
                onCanceled()
            }
        }
    }

    class Factory @Inject constructor(
        private val paymentMethodMetadata: PaymentMethodMetadata,
        private val tapToAddCollectionHandler: TapToAddCollectionHandler,
        private val eventReporter: EventReporter,
        private val stateHolder: TapToAddStateHolder,
        private val tapToAddCardCollectedScreenFactory: TapToAddCardCollectedScreenFactory,
        private val linkInlineSignupAvailability: LinkInlineSignupAvailability,
        private val navigator: Provider<TapToAddNavigator>,
        @UIContext private val uiContext: CoroutineContext,
        @IOContext private val ioContext: CoroutineContext,
        @Named(ENABLE_LOGGING) private val enableLogging: Boolean,
    ) : TapToAddCollectingInteractor.Factory {
        override fun create(): TapToAddCollectingInteractor {
            return DefaultTapToAddCollectingInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                uiContext = uiContext,
                ioContext = ioContext,
                tapToAddCollectionHandler = tapToAddCollectionHandler,
                linkInlineSignupAvailability = linkInlineSignupAvailability,
                onCollected = { paymentMethod ->
                    stateHolder.setState(TapToAddStateHolder.State.CardAdded(paymentMethod))

                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = tapToAddCardCollectedScreenFactory.create(paymentMethod),
                        )
                    )
                },
                onFailedCollection = { message ->
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.Error(message = message),
                        ),
                    )
                },
                onTapToAddNotSupported = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.NavigateTo(
                            screen = TapToAddNavigator.Screen.NotSupportedError,
                        ),
                    )
                },
                onCanceled = {
                    navigator.get().performAction(
                        action = TapToAddNavigator.Action.Close(),
                    )
                },
                eventReporter = eventReporter,
                logger = Logger.getInstance(enableLogging = enableLogging),
            )
        }
    }
}
