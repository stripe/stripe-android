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
    val customerSessionClientSecret: String?,
    val paymentMethods: List<PaymentMethod>,
    val defaultPaymentMethodId: String?,
) : Parcelable {

    @Parcelize
    sealed class DefaultPaymentMethodState : Parcelable {
        @Parcelize
        data class Enabled(val defaultPaymentMethodId: String?) : DefaultPaymentMethodState()

        @Parcelize
        data object Disabled : DefaultPaymentMethodState()
    }

    internal companion object {
        /**
         * Creates a [CustomerState] instance using an [ElementsSession.Customer] response.
         *
         * @param customer elements session customer data
         *
         * @return [CustomerState] instance using [ElementsSession.Customer] data
         */
        internal fun createForCustomerSession(
            customer: ElementsSession.Customer,
            supportedSavedPaymentMethodTypes: List<PaymentMethod.Type>,
            customerSessionClientSecret: String,
        ): CustomerState {
            return CustomerState(
                id = customer.session.customerId,
                ephemeralKeySecret = customer.session.apiKey,
                customerSessionClientSecret = customerSessionClientSecret,
                paymentMethods = customer.paymentMethods.filter {
                    supportedSavedPaymentMethodTypes.contains(it.type)
                },
                defaultPaymentMethodId = customer.defaultPaymentMethod
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
                customerSessionClientSecret = null,
                paymentMethods = paymentMethods,
                // This is a customer sessions only feature, so will always be null when using a legacy ephemeral key.
                defaultPaymentMethodId = null
            )
        }
    }
}
