package com.stripe.android.paymentsheet.elements;

import com.stripe.android.paymentsheet.elements.common.DropdownConfigInterface

internal class IdealBankConfig : DropdownConfigInterface {
    override val debugLabel = "idealBank"
    override val label = com.stripe.android.R.string.title_bank_account

    override fun convertToDisplay(paramFormatted: String?) =
        PARAM_TO_DISPLAY[paramFormatted] ?: ""

    override fun convertToPaymentMethodParam(displayFormatted: String) =
        DISPLAY_TO_PARAM[displayFormatted]

    override fun getItems(): List<String> = DISPLAY_TO_PARAM.keys.sortedBy { it }

    companion object {
        // TODO: Need to determine the correct way to pass junit default locale
        val DISPLAY_TO_PARAM: Map<String, String> = mapOf(
            "ABN AMRO" to "abn_amro",
            "ASN Bank" to "asn_bank",
            "Bunq" to "bunq",
            "Handelsbanken" to "handelsbanken",
            "ING" to "ing",
            "Knab" to "knab",
            "Moneyou" to "moneyou",
            "Rabobank" to "rabobank",
            "Revolut" to "revolut",
            "RegioBank" to "regiobank",
            "SNS Bank (De Volksbank)" to "sns_bank",
            "Triodos Bank" to "triodos_bank",
            "Van Lanschot" to "van_lanschot",
        )

        val PARAM_TO_DISPLAY = mapOf(
            "abn_amro" to "ABN AMRO",
            "asn_bank" to "ASN Bank",
            "bunq" to "Bunq",
            "handelsbanken" to "Handelsbanken",
            "ing" to "ING",
            "knab" to "Knab",
            "moneyou" to "Moneyou",
            "rabobank" to "Rabobank",
            "revolut" to "Revolut",
            "regiobank" to "RegioBank",
            "sns_bank" to "SNS Bank (De Volksbank)",
            "triodos_bank" to "Triodos Bank",
            "van_lanschot" to "Van Lanschot",
        )
    }
}
