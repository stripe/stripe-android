package com.stripe.android.paymentsheet.ui

internal fun String.readNumbersAsIndividualDigits(): String {
    // This makes the screen reader read out numbers digit by digit
    // one one one one vs one thousand one hundred eleven
    return replace("\\d".toRegex(), "$0 ")
}
