package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class, ExperimentalCustomerSessionApi::class)
@Singleton
internal class ElementsSessionManager @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (String) -> PrefsRepository,
    private val customerSessionProvider: CustomerSheet.CustomerSessionProvider,
    private val timeProvider: () -> Long,
    @IOContext private val workContext: CoroutineContext,
) {
    @Volatile
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey? = null

    private val isFetchingElementsSession = MutableStateFlow(false)

    suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey> {
        return withContext(workContext) {
            if (isFetchingElementsSession.value) {
                waitForElementsSessionToComplete()
            }

            cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                cachedCustomerEphemeralKey == null || cachedCustomerEphemeralKey.shouldRefresh(
                    timeProvider()
                )
            }?.let {
                Result.success(it)
            } ?: run {
                fetchElementsSession().getOrThrow()

                Result.success(cachedCustomerEphemeralKey ?: throw IllegalStateException("Should be available!"))
            }
        }
    }

    suspend fun fetchElementsSession(): Result<ElementsSession> {
        isFetchingElementsSession.value = true

        return withContext(workContext) {
            runCatching {
                val intentConfiguration = customerSessionProvider
                    .intentConfiguration()
                    .getOrThrow()

                val customerSessionClientSecret = customerSessionProvider
                    .providesCustomerSessionClientSecret()
                    .getOrThrow()

                val prefsRepository = prefsRepositoryFactory(customerSessionClientSecret.customerId)

                val savedSelection = prefsRepository.getSavedSelection(
                    isGooglePayAvailable = false,
                    isLinkAvailable = false,
                ) as? SavedSelection.PaymentMethod

                elementsSessionRepository.get(
                    initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                            paymentMethodTypes = intentConfiguration.paymentMethodTypes,
                        )
                    ),
                    defaultPaymentMethodId = savedSelection?.id,
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = customerSessionClientSecret.customerId,
                        clientSecret = customerSessionClientSecret.clientSecret,
                    ),
                    externalPaymentMethods = listOf(),
                ).onSuccess { elementsSession ->
                    elementsSession.customer?.session?.run {
                        cachedCustomerEphemeralKey = CachedCustomerEphemeralKey(
                            customerId = customerId,
                            ephemeralKey = apiKey,
                            expiresAt = apiKeyExpiry,
                        )
                    }
                }.getOrThrow()
            }
        }.also {
            isFetchingElementsSession.value = false
        }
    }

    private suspend fun waitForElementsSessionToComplete() {
        isFetchingElementsSession.first { isFetching ->
            !isFetching
        }
    }
}
