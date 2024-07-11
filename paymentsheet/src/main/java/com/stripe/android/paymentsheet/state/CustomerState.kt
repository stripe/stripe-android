package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerState(
    val id: String,
    val ephemeralKeySecret: String,
    val paymentMethods: List<PaymentMethod>,
    val permissions: Permissions,
) : Parcelable {
    @Parcelize
    data class Permissions(
        val canRemovePaymentMethods: Boolean,
        val canRemoveDuplicates: Boolean,
    ) : Parcelable

    internal companion object {
        /**
         * Creates a [CustomerState] instance using an [ElementsSession.Customer] response.
         *
         * @param customer elements session customer data
         *
         * @return [CustomerState] instance using [ElementsSession.Customer] data
         */
        internal fun createForCustomerSession(
            customer: ElementsSession.Customer
        ): CustomerState {
            val canRemovePaymentMethods = when (
                val paymentSheetComponent = customer.session.components.paymentSheet
            ) {
                is ElementsSession.Customer.Components.PaymentSheet.Enabled ->
                    paymentSheetComponent.isPaymentMethodRemoveEnabled
                is ElementsSession.Customer.Components.PaymentSheet.Disabled -> false
            }

            return CustomerState(
                id = customer.session.customerId,
                ephemeralKeySecret = customer.session.apiKey,
                paymentMethods = customer.paymentMethods,
                permissions = Permissions(
                    canRemovePaymentMethods = canRemovePaymentMethods,
                    // Should always remove duplicates when using `customer_session`
                    canRemoveDuplicates = true,
                )
            )
        }

        /**
         * Creates a [CustomerState] instance with un-scoped legacy ephemeral key information.
         *
         * @param customerId identifier for a customer
         * @param accessType legacy ephemeral key secret access type
         * @param paymentMethods list of payment methods belonging to the customer
         *
         * @return [CustomerState] instance with legacy ephemeral key secrets
         */
        internal fun createForLegacyEphemeralKey(
            customerId: String,
            accessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey,
            paymentMethods: List<PaymentMethod>,
        ): CustomerState {
            return CustomerState(
                id = customerId,
                ephemeralKeySecret = accessType.ephemeralKeySecret,
                paymentMethods = paymentMethods,
                permissions = Permissions(
                    /*
                     * Un-scoped legacy ephemeral keys have full permissions to remove/save/modify. This should
                     * always be set to true.
                     */
                    canRemovePaymentMethods = true,
                    /*
                     * Removing duplicates is not applicable here since we don't filter out duplicates for for
                     * un-scoped ephemeral keys.
                     */
                    canRemoveDuplicates = false,
                )
            )
        }
    }
}
