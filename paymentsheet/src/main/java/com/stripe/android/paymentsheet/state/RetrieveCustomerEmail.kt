package com.stripe.android.paymentsheet.state

import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.utils.DurationProvider
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject
import javax.inject.Provider

/**
 * Retrieves the customer email from any of the available sources.
 *
 * For [CustomerMetadata.CustomerSession] and [CustomerMetadata.LegacyEphemeralKey], checks the
 * default billing email first, then fetches from the [CustomerRepository].
 * For [CustomerMetadata.CheckoutSession] and null, returns the default billing email only.
 */
internal interface RetrieveCustomerEmail {

    suspend operator fun invoke(
        configuration: CommonConfiguration,
        customerMetadata: CustomerMetadata?,
        customerEmail: String?,
    ): String?
}

internal class DefaultRetrieveCustomerEmail @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val durationProvider: DurationProvider,
    private val paymentConfiguration: Provider<PaymentConfiguration>,
) : RetrieveCustomerEmail {

    override suspend operator fun invoke(
        configuration: CommonConfiguration,
        customerMetadata: CustomerMetadata?,
        customerEmail: String?,
    ): String? {
        return durationProvider.measureDuration(
            DurationProvider.Key.PaymentSheetLoadRetrieveCustomer,
        ) {
            val defaultEmail = configuration.defaultBillingDetails?.email
            when (customerMetadata) {
                is CustomerMetadata.CustomerSession -> {
                    defaultEmail ?: customerEmail
                }
                is CustomerMetadata.LegacyEphemeralKey -> {
                    defaultEmail ?: retrieveEmailFromApi(
                        customerId = customerMetadata.id,
                        ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
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
    ): String? {
        return customerRepository.retrieveCustomer(
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
            stripeAccountId = paymentConfiguration.get().stripeAccountId,
        )?.email
    }
}
