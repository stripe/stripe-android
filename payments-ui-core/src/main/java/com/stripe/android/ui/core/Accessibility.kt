package com.stripe.android.ui.core

internal fun String.asIndividualDigits(): String {
    return toCharArray().joinToString(separator = " ")
}
