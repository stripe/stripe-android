package com.stripe.android.paymentsheet.elements

internal fun convertTo4DigitDate(input: String) =
    "0$input".takeIf {
        (input.isNotBlank() && !(input[0] == '0' || input[0] == '1'))
            || ((input.length > 1) && (input[0] == '1' && requireNotNull(input[1].digitToInt()) > 2))
    } ?: input
