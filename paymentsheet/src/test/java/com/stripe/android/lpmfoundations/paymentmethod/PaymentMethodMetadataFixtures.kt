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
        id = "cus_123",
        ephemeralKeySecret = "ek_123",
        customerSessionClientSecret = null,
        isPaymentMethodSetAsDefaultEnabled = false,
        permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS
    )

    internal val CLIENT_ATTRIBUTION_METADATA = ClientAttributionMetadata(
        elementsSessionConfigId = "e961790f-43ed-4fcc-a534-74eeca28d042",
        paymentIntentCreationFlow = PaymentIntentCreationFlow.Standard,
        paymentMethodSelectionFlow = PaymentMethodSelectionFlow.Automatic,
    )

    internal val CUSTOMER_SESSIONS_CUSTOMER_METADATA = DEFAULT_CUSTOMER_METADATA.copy(
        customerSessionClientSecret = "cuss_123",
    )

    internal fun getDefaultCustomerMetadata(
        hasCustomerConfiguration: Boolean = true,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        permissions: CustomerMetadata.Permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
    ): CustomerMetadata? {
        return if (hasCustomerConfiguration) {
            DEFAULT_CUSTOMER_METADATA.copy(
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                permissions = permissions,
            )
        } else {
            null
        }
    }

    internal fun getDefaultCustomerMetadataFlow(
        hasCustomerConfiguration: Boolean = true,
        isPaymentMethodSetAsDefaultEnabled: Boolean = false,
        permissions: CustomerMetadata.Permissions = DEFAULT_CUSTOMER_METADATA_PERMISSIONS,
    ): StateFlow<CustomerMetadata?> {
        return stateFlowOf(
            getDefaultCustomerMetadata(
                hasCustomerConfiguration = hasCustomerConfiguration,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                permissions = permissions,
            )
        )
    }
}
