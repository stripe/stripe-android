package com.stripe.android.link

internal class FakeLinkConfigurationLoader : LinkConfigurationLoader {
    var shouldUpdateResult: Boolean = false

    var linkConfigurationResult: Result<LinkConfiguration> =
        Result.success(
            TestFactory.LINK_CONFIGURATION.copy(customerInfo = TestFactory.LINK_EMPTY_CUSTOMER_INFO)
        )

    override suspend fun load(configuration: LinkController.Configuration): Result<LinkConfiguration> {
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
                customerInfo = customerInfo ?: it.customerInfo,
                defaultBillingDetails = configuration.defaultBillingDetails,
                billingDetailsCollectionConfiguration = configuration.billingDetailsCollectionConfiguration
            )
        }
    }
}
