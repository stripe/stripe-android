package com.stripe.android.paymentsheet.model

import android.os.Parcelable
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
    val isGooglePayReady: Boolean,
    val savedSelection: SavedSelection
) : Parcelable
