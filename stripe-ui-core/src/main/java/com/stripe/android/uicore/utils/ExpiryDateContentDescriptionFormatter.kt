package com.stripe.android.uicore.utils

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.uicore.R
import java.text.SimpleDateFormat
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import java.util.Locale as JavaLocale

internal fun formatExpirationDateForAccessibility(locale: ComposeLocale, input: String): ResolvableString {
    if (input.isEmpty()) {
        return resolvableString(R.string.stripe_expiration_date_empty_content_description)
    }

    val canOnlyBeSingleDigitMonth = input.isNotBlank() && !(input[0] == '0' || input[0] == '1')
    val canOnlyBeJanuary = input.length > 1 && input.take(2).toInt() > 12
    val isSingleDigitMonth = canOnlyBeSingleDigitMonth || canOnlyBeJanuary

    val lastIndexOfMonth = if (isSingleDigitMonth) 0 else 1
    val month = input.take(lastIndexOfMonth + 1).toIntOrNull()
    val year = input.slice(lastIndexOfMonth + 1..input.lastIndex).toIntOrNull()

    try {
        val javaLocale = JavaLocale(locale.language, locale.region)
        if (month != null) {
            val monthName = SimpleDateFormat("MM", javaLocale).parse("$month")?.let {
                SimpleDateFormat("MMMM", javaLocale).format(it)
            }

            return when (year) {
                null -> return resolvableString(
                    R.string.stripe_expiration_date_month_complete_content_description,
                    monthName
                )
                in 0..9 -> resolvableString(
                    R.string.stripe_expiration_date_year_incomplete_content_description,
                    monthName
                )
                else -> resolvableString(
                    R.string.stripe_expiration_date_content_description,
                    monthName,
                    2000 + year
                )
            }
        }

        return input.resolvableString
    } catch (e: Exception) {
        return input.resolvableString
    }
}
