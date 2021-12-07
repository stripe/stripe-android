package com.stripe.android.ui.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

/**
 * This class represents the long value amount to charge and the currency code of the amount.
 */
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Amount(val value: Long, val currencyCode: String) : Parcelable
