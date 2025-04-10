package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
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
 * @param customer The customer information to retrieve the email from.
 * @return The customer's email if available, otherwise null.
 */
internal interface RetrieveCustomerEmail {

    suspend operator fun invoke(
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?
    ): String?
}

internal class DefaultRetrieveCustomerEmail @Inject constructor(
    private val customerRepository: CustomerRepository,
) : RetrieveCustomerEmail {

    override suspend operator fun invoke(
        configuration: CommonConfiguration,
        customer: CustomerRepository.CustomerInfo?
    ): String? {
        return configuration.defaultBillingDetails?.email
            ?: customer?.let { customerRepository.retrieveCustomer(it) }?.email
    }
}
