package com.stripe.android.lpmfoundations.paymentmethod

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.parcelize.Parcelize

/**
 * [PaymentMethodSaveConsentBehavior] defines the behavior to use when saving a payment method. This controls
 * whether or not the "Save for future use" checkbox should be shown.
 */
internal sealed interface PaymentMethodSaveConsentBehavior : Parcelable {
    /**
     * Default behavior for saving a payment method which shows a "Save for future use" checkbox when
     * in PI mode and has a customer to save the payment method to. PI w/SFU & SI modes do not show a checkbox.
     */
    @Parcelize
    data object Legacy : PaymentMethodSaveConsentBehavior

    /**
     * Behavior in which "Save for future use" is shown for all transaction modes (PI, PI w/SFU, and SI).
     */
    @Parcelize
    data object Enabled : PaymentMethodSaveConsentBehavior

    /**
     * Behavior in which "Save for future use" is shown for none of the transaction modes (PI, PI w/SFU, and SI).
     */
    @Parcelize
    data class Disabled(
        val overrideAllowRedisplay: PaymentMethod.AllowRedisplay?
    ) : PaymentMethodSaveConsentBehavior

    fun allowRedisplay(
        isSetupIntent: Boolean,
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentMethod.AllowRedisplay {
        return if (isSetupIntent) {
            allowRedisplayForSetupIntent(customerRequestedSave)
        } else {
            allowRedisplayForPaymentIntent(customerRequestedSave)
        }
    }

    private fun allowRedisplayForSetupIntent(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentMethod.AllowRedisplay {
        return when (this) {
            is Legacy -> PaymentMethod.AllowRedisplay.UNSPECIFIED
            is Disabled -> overrideAllowRedisplay ?: PaymentMethod.AllowRedisplay.LIMITED
            is Enabled -> {
                if (customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse) {
                    PaymentMethod.AllowRedisplay.ALWAYS
                } else {
                    PaymentMethod.AllowRedisplay.LIMITED
                }
            }
        }
    }

    private fun allowRedisplayForPaymentIntent(
        customerRequestedSave: PaymentSelection.CustomerRequestedSave,
    ): PaymentMethod.AllowRedisplay {
        return when (this) {
            is Legacy -> PaymentMethod.AllowRedisplay.UNSPECIFIED
            is Disabled -> PaymentMethod.AllowRedisplay.UNSPECIFIED
            is Enabled -> {
                if (customerRequestedSave == PaymentSelection.CustomerRequestedSave.RequestReuse) {
                    PaymentMethod.AllowRedisplay.ALWAYS
                } else {
                    PaymentMethod.AllowRedisplay.UNSPECIFIED
                }
            }
        }
    }
}
