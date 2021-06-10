package com.stripe.android.paymentsheet.elements.common

import kotlinx.coroutines.flow.Flow

interface Element {
    val label: Int
    val paymentMethodParams: Flow<String>
    val isComplete: Flow<Boolean>
    val errorMessage: Flow<Int?>
}