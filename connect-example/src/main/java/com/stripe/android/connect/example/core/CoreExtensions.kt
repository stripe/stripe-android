package com.stripe.android.connect.example.core

/**
 * Executes and returns the value of [block] if [this] is true, otherwise returns null.
 */
inline fun <T> Boolean.then(block: () -> T): T? = if (this) block() else null
