@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo

fun convertTo4DigitDate(input: String) =
    "0$input".takeIf {
        (input.isNotBlank() && !(input[0] == '0' || input[0] == '1')) ||
            ((input.length > 1) && (input[0] == '1' && requireNotNull(input[1].digitToInt()) > 2))
    } ?: input
