package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.model.ElementsSession
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class CustomerMetadata(
    val hasCustomerConfiguration: Boolean,
    val isPaymentMethodSetAsDefaultEnabled: Boolean,
    val permissions: Permissions,
) : Parcelable {

    @Parcelize
    internal data class Permissions(
        val removePaymentMethod: PaymentMethodRemovePermission,
        val canRemoveLastPaymentMethod: Boolean,
        val canRemoveDuplicates: Boolean,
        val canUpdateFullPaymentMethodDetails: Boolean,
    ) : Parcelable {
        val canRemovePaymentMethods: Boolean
            get() = removePaymentMethod == PaymentMethodRemovePermission.Full ||
                removePaymentMethod == PaymentMethodRemovePermission.Partial

        companion object {
            internal fun createForPaymentSheetCustomerSession(
                configuration: CommonConfiguration,
                customer: ElementsSession.Customer,
            ): Permissions {
                val mobilePaymentElementComponent = customer.session.components.mobilePaymentElement
                val removePaymentMethod = when (mobilePaymentElementComponent) {
                    is ElementsSession.Customer.Components.MobilePaymentElement.Enabled -> {
                        when (mobilePaymentElementComponent.paymentMethodRemove) {
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled ->
                                PaymentMethodRemovePermission.Full
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Partial ->
                                PaymentMethodRemovePermission.Partial
                            ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled ->
                                PaymentMethodRemovePermission.None
                        }
                    }
                    is ElementsSession.Customer.Components.MobilePaymentElement.Disabled ->
                        PaymentMethodRemovePermission.None
                }

                val canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod &&
                    mobilePaymentElementComponent is ElementsSession.Customer.Components.MobilePaymentElement.Enabled &&
                    mobilePaymentElementComponent.canRemoveLastPaymentMethod

                return Permissions(
                    removePaymentMethod = removePaymentMethod,
                    canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                    // Should always remove duplicates when using `customer_session`
                    canRemoveDuplicates = true,
                    // Should always be enabled when using `customer_session`
                    canUpdateFullPaymentMethodDetails = true,
                )
            }

            internal fun createForPaymentSheetLegacyEphemeralKey(
                configuration: CommonConfiguration,
            ): Permissions {
                return Permissions(
                    /*
                     * Un-scoped legacy ephemeral keys have full permissions to remove/save/modify. This should
                     * always be set to true.
                     */
                    removePaymentMethod = PaymentMethodRemovePermission.Full,
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
                    canUpdateFullPaymentMethodDetails = false,
                )
            }

            internal fun createForCustomerSheet(
                configuration: CustomerSheet.Configuration,
                customerSheetSession: CustomerSheetSession,
            ): Permissions {
                return Permissions(
                    removePaymentMethod = customerSheetSession.permissions.removePaymentMethod,
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails =
                    customerSheetSession.permissions.canUpdateFullPaymentMethodDetails,
                )
            }

            // Native link uses PaymentMethodMetadata for DefaultFormHelper and doesn't use CustomerMetadata at all
            internal fun createForNativeLink(): Permissions {
                return Permissions(
                    removePaymentMethod = PaymentMethodRemovePermission.None,
                    canRemoveLastPaymentMethod = false,
                    canRemoveDuplicates = false,
                    canUpdateFullPaymentMethodDetails = false
                )
            }
        }
    }
}
