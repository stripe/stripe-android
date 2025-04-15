package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.utils.FeatureFlags
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
    val permissions: Permissions,
    val defaultPaymentMethodId: String?,
) : Parcelable {
    @Parcelize
    data class Permissions(
        val canRemovePaymentMethods: Boolean,
        val canRemoveLastPaymentMethod: Boolean,
        val canRemoveDuplicates: Boolean,
        val canUpdatePaymentMethod: Boolean,
    ) : Parcelable

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
            configuration: CommonConfiguration,
            customer: ElementsSession.Customer,
            supportedSavedPaymentMethodTypes: List<PaymentMethod.Type>,
            customerSessionClientSecret: String,
        ): CustomerState {
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

            return CustomerState(
                id = customer.session.customerId,
                ephemeralKeySecret = customer.session.apiKey,
                customerSessionClientSecret = customerSessionClientSecret,
                paymentMethods = customer.paymentMethods.filter {
                    supportedSavedPaymentMethodTypes.contains(it.type)
                },
                permissions = Permissions(
                    canRemovePaymentMethods = canRemovePaymentMethods,
                    canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                    // Should always remove duplicates when using `customer_session`
                    canRemoveDuplicates = true,
                    canUpdatePaymentMethod = FeatureFlags.editSavedCardPaymentMethodEnabled.isEnabled,
                ),
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
            configuration: CommonConfiguration,
            customerId: String,
            accessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey,
            paymentMethods: List<PaymentMethod>,
        ): CustomerState {
            return CustomerState(
                id = customerId,
                ephemeralKeySecret = accessType.ephemeralKeySecret,
                customerSessionClientSecret = null,
                paymentMethods = paymentMethods,
                permissions = Permissions(
                    /*
                     * Un-scoped legacy ephemeral keys have full permissions to remove/save. This should
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
                    /*
                     * Un-scoped legacy ephemeral keys do not have permissions to update payment method. This should
                     * always be set to false.
                     */
                    canUpdatePaymentMethod = false,
                ),
                // This is a customer sessions only feature, so will always be null when using a legacy ephemeral key.
                defaultPaymentMethodId = null
            )
        }
    }
}
