package com.stripe.android.googlepaysheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for [GooglePaySheet].
 */
@Parcelize
data class GooglePaySheetConfiguration(
    /**
     * The ISO 4217 alphabetic currency code.
     */
    val currencyCode: String,

    /**
     * The ISO 3166-1 alpha-2 country code where the transaction is processed.
     */
    val countryCode: String,

    /**
     * Your customer-facing business name.
     *
     * The default value is the name of your app.
     */
    val merchantDisplayName: String,
) : Parcelable
