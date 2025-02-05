package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.CustomerState.Permissions

internal data class CustomerMetadata(
    val id: String,
    val ephemeralKeySecret: String,
    val customerSessionClientSecret: String?,
    val permissions: Permissions,
    val isDefaultPaymentMethodEnabled: Boolean
) {
    internal companion object {
        internal fun createForCustomerSession(
            configuration: CommonConfiguration,
            customer: ElementsSession.Customer,
            customerSessionClientSecret: String,
        ): CustomerMetadata {
            val mobilePaymentElementComponent = customer.session.components.mobilePaymentElement

            val canRemovePaymentMethods = when (mobilePaymentElementComponent) {
                is ElementsSession.Customer.Components.MobilePaymentElement.Enabled -> {
                    mobilePaymentElementComponent.isPaymentMethodRemoveEnabled
                }
                is ElementsSession.Customer.Components.MobilePaymentElement.Disabled -> false
            }

            val canRemoveLastPaymentMethod = when {
                !configuration.allowsRemovalOfLastSavedPaymentMethod -> false
                mobilePaymentElementComponent is ElementsSession.Customer.Components.MobilePaymentElement.Enabled ->
                    mobilePaymentElementComponent.canRemoveLastPaymentMethod
                else -> false
            }

            val isDefaultPaymentMethodEnabled = FeatureFlags.enableDefaultPaymentMethods.isEnabled

            return CustomerMetadata(
                id = customer.session.customerId,
                ephemeralKeySecret = customer.session.apiKey,
                customerSessionClientSecret = customerSessionClientSecret,
                permissions = Permissions(
                    canRemovePaymentMethods = canRemovePaymentMethods,
                    canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                    // Should always remove duplicates when using `customer_session`
                    canRemoveDuplicates = true,
                ),
                isDefaultPaymentMethodEnabled = isDefaultPaymentMethodEnabled,
            )
        }

        internal fun createForLegacyEphemeralKey(
            configuration: CommonConfiguration,
            customerId: String,
            accessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey,
        ): CustomerMetadata {
            return CustomerMetadata(
                id = customerId,
                ephemeralKeySecret = accessType.ephemeralKeySecret,
                customerSessionClientSecret = null,
                permissions = Permissions(
                    /*
                     * Un-scoped legacy ephemeral keys have full permissions to remove/save/modify. This should
                     * always be set to true.
                     */
                    canRemovePaymentMethods = true,
                    /*
                     * Un-scoped legacy ephemeral keys normally have full permissions to remove the last payment
                     * method, however we do have client-side configuration option to configure this ability. This
                     * should eventually be removed in favor of the server-side option available with customer
                     * sessions.
                     */
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    /*
                     * Removing duplicates is not applicable here since we don't filter out duplicates for for
                     * un-scoped ephemeral keys.
                     */
                    canRemoveDuplicates = false,
                ),
                isDefaultPaymentMethodEnabled = false,
            )
        }
    }
}
