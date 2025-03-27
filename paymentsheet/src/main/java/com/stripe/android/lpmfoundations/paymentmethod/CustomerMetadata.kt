package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
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
        val canRemovePaymentMethods: Boolean,
        val canRemoveLastPaymentMethod: Boolean,
        val canRemoveDuplicates: Boolean,
    ) : Parcelable {
        companion object {
            internal fun createForPaymentSheetCustomerSession(
                configuration: CommonConfiguration,
                customer: ElementsSession.Customer,
            ): Permissions {
                val mobilePaymentElementComponent = customer.session.components.mobilePaymentElement
                val canRemovePaymentMethods = when (mobilePaymentElementComponent) {
                    is ElementsSession.Customer.Components.MobilePaymentElement.Enabled -> {
                        mobilePaymentElementComponent.isPaymentMethodRemoveEnabled
                    }
                    is ElementsSession.Customer.Components.MobilePaymentElement.Disabled -> false
                }

                val canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod &&
                    mobilePaymentElementComponent is ElementsSession.Customer.Components.MobilePaymentElement.Enabled &&
                    mobilePaymentElementComponent.canRemoveLastPaymentMethod

                return Permissions(
                    canRemovePaymentMethods = canRemovePaymentMethods,
                    canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                    // Should always remove duplicates when using `customer_session`
                    canRemoveDuplicates = true,
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
                )
            }

            internal fun createForCustomerSheet(
                configuration: CustomerSheet.Configuration,
                customerSheetSession: CustomerSheetSession,
            ): Permissions {
                return Permissions(
                    canRemovePaymentMethods = customerSheetSession.permissions.canRemovePaymentMethods,
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    canRemoveDuplicates = true,
                )
            }

            // Native link uses PaymentMethodMetadata for DefaultFormHelper and doesn't use CustomerMetadata at all
            internal fun createForNativeLink(): Permissions {
                return Permissions(
                    canRemovePaymentMethods = false,
                    canRemoveLastPaymentMethod = false,
                    canRemoveDuplicates = false,
                )
            }
        }
    }
}
