package com.stripe.android.financialconnections.utils

import androidx.annotation.RestrictTo
import kotlin.collections.iterator

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> =
    buildMap { for ((k, v) in this@filterNotNullValues) if (v != null) put(k, v) }
