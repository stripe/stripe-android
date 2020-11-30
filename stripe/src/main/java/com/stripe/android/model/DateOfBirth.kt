package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_DAY to day,
            PARAM_MONTH to month,
            PARAM_YEAR to year
        )
    }

    private companion object {
        private const val PARAM_DAY = "day"
        private const val PARAM_MONTH = "month"
        private const val PARAM_YEAR = "year"
    }
}
