package com.stripe.example.activity

internal object BackgroundTaskTracker {
    internal var onStart: () -> Unit = {}
    internal var onStop: () -> Unit = {}

    internal fun reset() {
        onStart = {}
        onStop = {}
    }
}
