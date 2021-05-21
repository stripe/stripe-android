package com.stripe.android.paymentsheet.repositories

import com.stripe.android.paymentsheet.model.FragmentConfig

internal interface FragmentConfigRepository {
    suspend fun get(): FragmentConfig
}
