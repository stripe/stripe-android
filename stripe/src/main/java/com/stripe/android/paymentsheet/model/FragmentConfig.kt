package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.BaseAddCardFragment
import com.stripe.android.paymentsheet.BasePaymentMethodsListFragment
import kotlinx.parcelize.Parcelize

/**
 * Configuration data for [BaseAddCardFragment] and [BasePaymentMethodsListFragment].
 */
@Parcelize
internal data class FragmentConfig(
    val paymentIntent: PaymentIntent,
    val paymentMethods: List<PaymentMethod>,
    val isGooglePayReady: Boolean,
    val savedSelection: SavedSelection
) : Parcelable {
    val shouldShowGooglePayButton: Boolean
        get() = isGooglePayReady && paymentMethods.isEmpty()

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
