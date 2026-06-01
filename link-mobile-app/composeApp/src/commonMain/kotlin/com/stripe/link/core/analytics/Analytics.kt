package com.stripe.link.core.analytics

/**
 * Lightweight analytics facade for breadcrumb logging.
 */
object Analytics {
    fun logBreadcrumb(message: String) {
        // Platform-specific implementations plug in via expect/actual
        println("[Analytics] $message")
    }
}
