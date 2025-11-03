package com.stripe.android.link

import com.stripe.android.link.injection.LinkMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory

internal class FakeLinkConfigurationLoader : LinkConfigurationLoader {
    var shouldUpdateResult: Boolean = false

    var linkConfigurationResult: Result<LinkMetadata> =
        Result.success(
            LinkMetadata(
                TestFactory.LINK_CONFIGURATION.copy(customerInfo = TestFactory.LINK_EMPTY_CUSTOMER_INFO),
                PaymentMethodMetadataFactory.create()
            )
        )

    override suspend fun load(configuration: LinkController.Configuration): Result<LinkMetadata> {
        if (!shouldUpdateResult) {
            return linkConfigurationResult
        }

        val customerInfo = configuration.defaultBillingDetails?.let {
            LinkConfiguration.CustomerInfo(
                name = it.name,
                email = it.email,
                phone = it.phone,
                billingCountryCode = it.address?.country
            )
        }
        return linkConfigurationResult.map {
            it.copy(
                linkConfiguration = it.linkConfiguration.copy(
                    customerInfo = customerInfo ?: it.linkConfiguration.customerInfo,
                    defaultBillingDetails = configuration.defaultBillingDetails,
                    billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration
                )
            )
        }
    }
}
