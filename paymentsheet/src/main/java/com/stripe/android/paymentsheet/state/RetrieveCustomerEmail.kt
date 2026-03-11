package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject

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
    ): String?
}

internal class DefaultRetrieveCustomerEmail @Inject constructor(
    private val customerRepository: CustomerRepository,
) : RetrieveCustomerEmail {

    override suspend operator fun invoke(
        configuration: CommonConfiguration,
        customerMetadata: CustomerMetadata?,
    ): String? {
        val defaultEmail = configuration.defaultBillingDetails?.email
        return when (customerMetadata) {
            is CustomerMetadata.CustomerSession -> {
                defaultEmail ?: retrieveEmailFromApi(
                    customerId = customerMetadata.id,
                    ephemeralKeySecret = customerMetadata.ephemeralKeySecret,
                )
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

    private suspend fun retrieveEmailFromApi(
        customerId: String,
        ephemeralKeySecret: String,
    ): String? {
        return customerRepository.retrieveCustomer(
            customerId = customerId,
            ephemeralKeySecret = ephemeralKeySecret,
        )?.email
    }
}
