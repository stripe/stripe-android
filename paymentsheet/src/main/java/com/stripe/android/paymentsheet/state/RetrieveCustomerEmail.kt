package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject

/** Resolves the Link email from the initialization context and customer access. */
internal interface RetrieveCustomerEmail {

    suspend operator fun invoke(
        configuration: CommonConfiguration,
        initializationMode: PaymentElementLoader.InitializationMode,
        customerMetadata: CustomerMetadata?,
        customerEmail: String?,
        apiConfiguration: ApiConfiguration.State,
    ): String?
}

internal class DefaultRetrieveCustomerEmail @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val durationProvider: DurationProvider,
) : RetrieveCustomerEmail {

    override suspend operator fun invoke(
        configuration: CommonConfiguration,
        initializationMode: PaymentElementLoader.InitializationMode,
        customerMetadata: CustomerMetadata?,
        customerEmail: String?,
        apiConfiguration: ApiConfiguration.State,
    ): String? {
        return durationProvider.measureDuration(
            DurationProvider.Key.PaymentSheetLoadRetrieveCustomer,
        ) {
            if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
                return@measureDuration initializationMode.checkoutSessionResponse.fixedEmail
                    ?: initializationMode.collectedEmail
            }
            val defaultEmail = configuration.defaultBillingDetails?.email
            when (customerMetadata) {
                is CustomerMetadata.CustomerSession -> {
                    defaultEmail ?: customerEmail
                }
                is CustomerMetadata.LegacyEphemeralKey -> {
                    defaultEmail ?: retrieveEmailFromApi(
                        customerId = customerMetadata.id,
                        ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                        apiConfiguration = apiConfiguration,
                    )
                }
                is CustomerMetadata.CheckoutSession,
                null -> defaultEmail
            }
        }
    }

    private suspend fun retrieveEmailFromApi(
        customerId: String,
        ephemeralKeySecret: String,
        apiConfiguration: ApiConfiguration.State,
    ): String? {
        return customerRepository.retrieveCustomer(
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            apiConfiguration = apiConfiguration,
        )?.email
    }
}
