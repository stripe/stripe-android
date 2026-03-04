package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSession.Customer.Components.MobilePaymentElement
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class CustomerMetadata : Parcelable {
    abstract val removePaymentMethod: PaymentMethodRemovePermission
    abstract val saveConsent: PaymentMethodSaveConsentBehavior
    abstract val canRemoveLastPaymentMethod: Boolean
    abstract val canRemoveDuplicates: Boolean
    abstract val canUpdateFullPaymentMethodDetails: Boolean
    abstract val isPaymentMethodSetAsDefaultEnabled: Boolean

    val canRemovePaymentMethods: Boolean
        get() = removePaymentMethod == PaymentMethodRemovePermission.Full ||
            removePaymentMethod == PaymentMethodRemovePermission.Partial

    @Parcelize
    data class LegacyEphemeralKey(
        val id: String,
        val ephemeralKeySecret: String,
        override val removePaymentMethod: PaymentMethodRemovePermission,
        override val saveConsent: PaymentMethodSaveConsentBehavior,
        override val canRemoveLastPaymentMethod: Boolean,
        override val canRemoveDuplicates: Boolean,
        override val canUpdateFullPaymentMethodDetails: Boolean,
        override val isPaymentMethodSetAsDefaultEnabled: Boolean,
    ) : CustomerMetadata()

    @Parcelize
    data class Session(
        val id: String,
        val ephemeralKeySecret: String,
        val customerSessionClientSecret: String,
        override val removePaymentMethod: PaymentMethodRemovePermission,
        override val saveConsent: PaymentMethodSaveConsentBehavior,
        override val canRemoveLastPaymentMethod: Boolean,
        override val canRemoveDuplicates: Boolean,
        override val canUpdateFullPaymentMethodDetails: Boolean,
        override val isPaymentMethodSetAsDefaultEnabled: Boolean,
    ) : CustomerMetadata()

    @Parcelize
    data class CheckoutSession(
        val sessionId: String,
        override val removePaymentMethod: PaymentMethodRemovePermission,
        override val saveConsent: PaymentMethodSaveConsentBehavior,
        override val canRemoveLastPaymentMethod: Boolean,
        override val canRemoveDuplicates: Boolean,
        override val canUpdateFullPaymentMethodDetails: Boolean,
        override val isPaymentMethodSetAsDefaultEnabled: Boolean,
    ) : CustomerMetadata()

    companion object {
        internal fun createForPaymentSheetCustomerSession(
            configuration: CommonConfiguration,
            customer: ElementsSession.Customer,
            id: String,
            ephemeralKeySecret: String,
            customerSessionClientSecret: String,
            isPaymentMethodSetAsDefaultEnabled: Boolean,
        ): Session {
            val mobilePaymentElementComponent = customer.session.components.mobilePaymentElement
            val removePaymentMethod = when (mobilePaymentElementComponent) {
                is MobilePaymentElement.Enabled -> {
                    when (mobilePaymentElementComponent.paymentMethodRemove) {
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Enabled ->
                            PaymentMethodRemovePermission.Full
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Partial ->
                            PaymentMethodRemovePermission.Partial
                        ElementsSession.Customer.Components.PaymentMethodRemoveFeature.Disabled ->
                            PaymentMethodRemovePermission.None
                    }
                }
                is MobilePaymentElement.Disabled ->
                    PaymentMethodRemovePermission.None
            }

            val canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod &&
                mobilePaymentElementComponent is MobilePaymentElement.Enabled &&
                mobilePaymentElementComponent.canRemoveLastPaymentMethod

            val saveConsent = when (mobilePaymentElementComponent) {
                is MobilePaymentElement.Enabled -> {
                    if (mobilePaymentElementComponent.isPaymentMethodSaveEnabled) {
                        PaymentMethodSaveConsentBehavior.Enabled
                    } else {
                        PaymentMethodSaveConsentBehavior.Disabled(
                            overrideAllowRedisplay = mobilePaymentElementComponent.allowRedisplayOverride
                        )
                    }
                }
                is MobilePaymentElement.Disabled -> {
                    PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null)
                }
            }

            return Session(
                id = id,
                ephemeralKeySecret = ephemeralKeySecret,
                customerSessionClientSecret = customerSessionClientSecret,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                removePaymentMethod = removePaymentMethod,
                saveConsent = saveConsent,
                canRemoveLastPaymentMethod = canRemoveLastPaymentMethod,
                // Should always remove duplicates when using `customer_session`
                canRemoveDuplicates = true,
                // Should always be enabled when using `customer_session`
                canUpdateFullPaymentMethodDetails = true,
            )
        }

        internal fun createForPaymentSheetLegacyEphemeralKey(
            configuration: CommonConfiguration,
            id: String,
            ephemeralKeySecret: String,
            isPaymentMethodSetAsDefaultEnabled: Boolean,
        ): LegacyEphemeralKey {
            return LegacyEphemeralKey(
                id = id,
                ephemeralKeySecret = ephemeralKeySecret,
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                /*
                 * Un-scoped legacy ephemeral keys have full permissions to remove/save/modify. This should
                 * always be set to true.
                 */
                removePaymentMethod = PaymentMethodRemovePermission.Full,
                /*
                 * Legacy ephemeral keys don't have server-side save consent configuration, so we use
                 * the legacy behavior which shows the save checkbox based on the setup intent usage.
                 */
                saveConsent = PaymentMethodSaveConsentBehavior.Legacy,
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
            id: String,
            ephemeralKeySecret: String,
            customerSessionClientSecret: String?,
            isPaymentMethodSetAsDefaultEnabled: Boolean,
        ): CustomerMetadata {
            return if (customerSessionClientSecret != null) {
                Session(
                    id = id,
                    ephemeralKeySecret = ephemeralKeySecret,
                    customerSessionClientSecret = customerSessionClientSecret,
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                    removePaymentMethod = customerSheetSession.permissions.removePaymentMethod,
                    saveConsent = customerSheetSession.paymentMethodSaveConsentBehavior,
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails =
                        customerSheetSession.permissions.canUpdateFullPaymentMethodDetails,
                )
            } else {
                LegacyEphemeralKey(
                    id = id,
                    ephemeralKeySecret = ephemeralKeySecret,
                    isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled,
                    removePaymentMethod = customerSheetSession.permissions.removePaymentMethod,
                    saveConsent = customerSheetSession.paymentMethodSaveConsentBehavior,
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    canRemoveDuplicates = true,
                    canUpdateFullPaymentMethodDetails =
                        customerSheetSession.permissions.canUpdateFullPaymentMethodDetails,
                )
            }
        }
    }
}
