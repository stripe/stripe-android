package com.stripe.android.paymentsheet.state

import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.core.injection.IOContext
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.confirmation.utils.toInitializationMode
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal interface PaymentMethodRefresher {
    suspend fun refresh(metadata: PaymentMethodMetadata): Result<List<PaymentMethod>>

    sealed interface CustomerInfo {
        val customerId: String

        class CustomerSession(
            override val customerId: String,
            val customerSessionClientSecret: String,
        ) : CustomerInfo

        class Legacy(
            override val customerId: String,
            val ephemeralKeySecret: String,
        ) : CustomerInfo
    }
}

internal class DefaultPaymentMethodRefresher @Inject constructor(
    @IOContext private val workContext: CoroutineContext,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerRepository: CustomerRepository,
    private val paymentMethodFilter: PaymentMethodFilter,
    private val prefsRepositoryFactory: PrefsRepository.Factory,
) : PaymentMethodRefresher {
    override suspend fun refresh(
        metadata: PaymentMethodMetadata
    ): Result<List<PaymentMethod>> = workContext.runCatching {
        val customerMetadata = metadata.customerMetadata
            ?: return@runCatching emptyList()

        val localSavedSelection = async {
            getLocalSavedSelection(customerMetadata.id)
        }

        val (paymentMethods, remoteDefaultPaymentMethodId) = customerMetadata.customerSessionClientSecret?.let {
            val elementsSessionCustomer = getElementsSessionCustomer(
                customerId = customerMetadata.id,
                customerSessionClientSecret = it,
                metadata = metadata,
                localSavedSelection = localSavedSelection,
            )

            val paymentMethods = elementsSessionCustomer?.paymentMethods ?: emptyList()

            paymentMethods to elementsSessionCustomer?.defaultPaymentMethod
        } ?: run {
            getLegacyPaymentMethods(
                customerId = customerMetadata.id,
                ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                metadata = metadata,
            ) to null
        }

        return@runCatching filter(
            paymentMethods = paymentMethods,
            metadata = metadata,
            remoteDefaultPaymentMethodId = remoteDefaultPaymentMethodId,
            customerMetadata = customerMetadata,
            localSavedSelection = localSavedSelection,
        )
    }

    private suspend fun getElementsSessionCustomer(
        customerId: String,
        customerSessionClientSecret: String,
        metadata: PaymentMethodMetadata,
        localSavedSelection: Deferred<SavedSelection>
    ): ElementsSession.Customer? {
        val initializationMode = metadata.integrationMetadata.toInitializationMode(metadata.stripeIntent)
            ?: return null

        val elementsSession = elementsSessionRepository.get(
            customPaymentMethods = emptyList(),
            externalPaymentMethods = emptyList(),
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = customerId,
                clientSecret = customerSessionClientSecret,
            ),
            initializationMode = initializationMode,
            countryOverride = metadata.userOverrideCountry,
            savedPaymentMethodSelectionId = localSavedSelection.await().asPaymentMethod()?.id,
        ).getOrThrow()

        elementsSession.sessionsError?.let {
            throw it
        }

        return elementsSession.customer
    }

    private suspend fun getLegacyPaymentMethods(
        customerId: String,
        ephemeralKeySecret: String,
        metadata: PaymentMethodMetadata,
    ): List<PaymentMethod> {
        return customerRepository.getPaymentMethods(
            customerInfo = CustomerRepository.CustomerInfo(
                id = customerId,
                ephemeralKeySecret = ephemeralKeySecret,
                customerSessionClientSecret = null,
            ),
            types = metadata.supportedSavedPaymentMethodTypes(),
            silentlyFail = false,
        ).getOrThrow()
    }

    private suspend fun filter(
        paymentMethods: List<PaymentMethod>,
        metadata: PaymentMethodMetadata,
        customerMetadata: CustomerMetadata,
        remoteDefaultPaymentMethodId: String?,
        localSavedSelection: Deferred<SavedSelection>
    ): List<PaymentMethod> {
        return paymentMethodFilter.filter(
            paymentMethods = paymentMethods,
            params = PaymentMethodFilter.FilterParams(
                billingDetailsCollectionConfiguration = metadata.billingDetailsCollectionConfiguration,
                customerMetadata = customerMetadata,
                remoteDefaultPaymentMethodId = remoteDefaultPaymentMethodId,
                cardBrandFilter = metadata.cardBrandFilter,
                cardFundingFilter = metadata.cardFundingFilter,
                localSavedSelection = localSavedSelection,
            )
        )
    }

    private suspend fun getLocalSavedSelection(
        customerId: String,
    ): SavedSelection {
        return prefsRepositoryFactory.create(customerId).getSavedSelection(
            isGooglePayAvailable = false,
            isLinkAvailable = false
        )
    }

    private fun SavedSelection.asPaymentMethod(): SavedSelection.PaymentMethod? {
        return this as? SavedSelection.PaymentMethod
    }
}
