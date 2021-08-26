package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * This class represents the long value amount to charge and the currency code of the amount.
 */
@Parcelize
internal data class Amount(val value: Long, val currencyCode: String) : Parcelable
