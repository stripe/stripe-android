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
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface CustomerSessionElementsSessionManager {
    suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey>

    suspend fun fetchElementsSession(): Result<ElementsSession>
}

@OptIn(ExperimentalCustomerSheetApi::class, ExperimentalCustomerSessionApi::class)
@Singleton
internal class DefaultCustomerSessionElementsSessionManager @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (String) -> PrefsRepository,
    private val customerSessionProvider: CustomerSheet.CustomerSessionProvider,
    private val timeProvider: () -> Long,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSessionElementsSessionManager {
    @Volatile
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey? = null

    private var intentConfiguration: CustomerSheet.IntentConfiguration? = null

    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey> {
        return withContext(workContext) {
            cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                cachedCustomerEphemeralKey == null || cachedCustomerEphemeralKey.shouldRefresh(
                    timeProvider()
                )
            }?.let {
                Result.success(it)
            } ?: run {
                fetchElementsSession().mapCatching {
                    cachedCustomerEphemeralKey
                        ?: throw IllegalStateException("Should have been initialized from `elements/session`!")
                }
            }
        }
    }

    override suspend fun fetchElementsSession(): Result<ElementsSession> {
        return withContext(workContext) {
            runCatching {
                val intentConfiguration = intentConfiguration
                    ?: customerSessionProvider.intentConfiguration()
                        .onSuccess { intentConfiguration = it }
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
        }
    }
}
