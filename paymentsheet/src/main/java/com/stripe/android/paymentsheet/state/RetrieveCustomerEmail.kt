package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import javax.inject.Inject

/**
 * Retrieves the customer email from any of the available sources.
 *
 * This function first checks the [CommonConfiguration] for a default email.
 * If not found, it attempts to retrieve the email from the [CustomerRepository].
 *
 * @param customerRepository The repository to fetch customer information.
 * @param configuration The common configuration containing default billing details.
 * @param accessInfo The access info to retrieve the customer email from.
 * @return The customer's email if available, otherwise null.
 */
internal interface RetrieveCustomerEmail {

    suspend operator fun invoke(
        configuration: CommonConfiguration,
        accessInfo: CustomerMetadata.AccessInfo?,
    ): String?
}

internal class DefaultRetrieveCustomerEmail @Inject constructor(
    private val customerRepository: CustomerRepository,
) : RetrieveCustomerEmail {

    override suspend operator fun invoke(
        configuration: CommonConfiguration,
        accessInfo: CustomerMetadata.AccessInfo?,
    ): String? {
        return configuration.defaultBillingDetails?.email
            ?: if (accessInfo != null && accessInfo.ephemeralKeySecret != null) {
                customerRepository.retrieveCustomer(accessInfo)?.email
            } else {
                null
            }
    }
}
