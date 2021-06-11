package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import kotlinx.parcelize.Parcelize

/**
 * Configuration data for [BaseAddPaymentMethodFragment] and [BasePaymentMethodsListFragment] and
 * their subclasses.
 */
@Parcelize
internal data class FragmentConfig(
    val stripeIntent: StripeIntent,
    val paymentMethods: List<PaymentMethod>,
    val isGooglePayReady: Boolean,
    val savedSelection: SavedSelection
) : Parcelable {

    val sortedPaymentMethods: List<PaymentMethod>
        get() {
            val primaryPaymentMethodIndex = when (savedSelection) {
                is SavedSelection.PaymentMethod -> {
                    paymentMethods.indexOfFirst {
                        it.id == savedSelection.id
                    }
                }
                else -> -1
            }
            return if (primaryPaymentMethodIndex != -1) {
                val mutablePaymentMethods = paymentMethods.toMutableList()
                mutablePaymentMethods.removeAt(primaryPaymentMethodIndex)
                    .also { primaryPaymentMethod ->
                        mutablePaymentMethods.add(0, primaryPaymentMethod)
                    }
                mutablePaymentMethods
            } else {
                paymentMethods
            }
        }
}
