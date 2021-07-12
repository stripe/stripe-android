package com.stripe.android.paymentsheet.elements

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class SectionController(
    val label: Int?
) {
    // This should be based off the fields in the section?
    val errorMessage: Flow<SectionError?> = MutableStateFlow(null)
}


data class SectionError(
    @StringRes val errorFieldLabel: Int,
    @StringRes val errorMessage: Int
)
