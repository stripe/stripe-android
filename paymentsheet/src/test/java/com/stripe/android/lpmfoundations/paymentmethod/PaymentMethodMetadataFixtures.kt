package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal object PaymentMethodMetadataFixtures {

    internal val DEFAULT_CUSTOMER_METADATA_PERMISSIONS = CustomerMetadata.Permissions(
        canRemovePaymentMethods = true,
        canRemoveLastPaymentMethod = true,
        canRemoveDuplicates = true,
    )

    internal val DEFAULT_CUSTOMER_METADATA = CustomerMetadata(
        hasCustomerConfiguration = true,
        isPaymentMethodSetAsDefaultEnabled = false,
        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS
    )

    internal fun getDefaultCustomerMetadata(
        hasCustomerConfiguration: Boolean = true,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        permissions: CustomerMetadata.Permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
    ): CustomerMetadata {
        return CustomerMetadata(
            hasCustomerConfiguration = hasCustomerConfiguration,
            isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
            permissions = permissions,
        )
    }

    internal fun getDefaultCustomerMetadataFlow(
        hasCustomerConfiguration: Boolean = true,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        permissions: CustomerMetadata.Permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
    ): StateFlow<CustomerMetadata> {
        return stateFlowOf(
            getDefaultCustomerMetadata(
                hasCustomerConfiguration = hasCustomerConfiguration,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                permissions = permissions,
            )
        )
    }
}
