package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal object PaymentMethodMetadataFixtures {

    internal val DEFAULT_CUSTOMER_METADATA_PERMISSIONS = CustomerMetadata.Permissions(
        removePaymentMethod = PaymentMethodRemovePermission.Full,
        canRemoveLastPaymentMethod = true,
        canRemoveDuplicates = true,
        canUpdateFullPaymentMethodDetails = false,
    )

    internal val DEFAULT_CUSTOMER_METADATA = CustomerMetadata(
        hasCustomerConfiguration = true,
        isPaymentMethodSetAsDefaultEnabled = false,
        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS
    )

    internal val CLIENT_ATTRIBUTION_METADATA = ClientAttributionMetadata(
        elementsSessionConfigId = "elements_session_123",
        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
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
