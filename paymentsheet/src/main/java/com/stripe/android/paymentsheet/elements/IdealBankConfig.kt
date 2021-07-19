package com.stripe.android.paymentsheet.elements

import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import com.stripe.android.paymentsheet.R

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class IdealBankConfig : DropdownConfig {
    override val debugLabel = "idealBank"

    @StringRes
    override val label = R.string.stripe_paymentsheet_ideal_bank

    override fun getDisplayItems() = DISPLAY_TO_PARAM.map { it.displayName }
    override fun convertFromRaw(rawValue: String) = DISPLAY_TO_PARAM
        .firstOrNull { it.paymentMethodParamFieldValue == rawValue }
        ?.paymentMethodParamFieldValue ?: DISPLAY_TO_PARAM[0].displayName

    override fun convertToRaw(displayName: String) = DISPLAY_TO_PARAM
        .firstOrNull { it.displayName == displayName }
        ?.paymentMethodParamFieldValue

    companion object {
        val DISPLAY_TO_PARAM: List<Item> = listOf(
            Item("ABN AMRO", "abn_amro"),
            Item("ASN Bank", "asn_bank"),
            Item("Bunq", "bunq"),
            Item("Handelsbanken", "handelsbanken"),
            Item("ING", "ing"),
            Item("Knab", "knab"),
            Item("Rabobank", "rabobank"),
            Item("Revolut", "revolut"),
            Item("RegioBank", "regiobank"),
            Item("SNS Bank (De Volksbank)", "sns_bank"),
            Item("Triodos Bank", "triodos_bank"),
            Item("Van Lanschot", "van_lanschot"),
        )
    }

    data class Item(val displayName: String, val paymentMethodParamFieldValue: String)
}
