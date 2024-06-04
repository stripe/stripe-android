package com.stripe.android.financialconnections.utils

internal inline fun <T> measureTimeMillis(
    function: () -> T
): Pair<T, Long> {
    val startTime = System.currentTimeMillis()
    val result: T = function.invoke()

    return result to (System.currentTimeMillis() - startTime)
}
