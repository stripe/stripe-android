package com.stripe.android.core.utils

import java.util.Calendar

internal actual fun currentYearMonth(): CurrentYearMonth {
    val calendar = Calendar.getInstance()
    return CurrentYearMonth(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1
    )
}
