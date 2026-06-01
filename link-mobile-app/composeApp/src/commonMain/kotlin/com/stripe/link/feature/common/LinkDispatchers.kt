package com.stripe.link.feature.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Coroutine dispatcher bundle used throughout the app.
 * Injected to allow test overrides.
 */
data class LinkDispatchers(
    val Main: CoroutineDispatcher = Dispatchers.Main,
    val IO: CoroutineDispatcher = Dispatchers.Default,
)
