package com.stripe.android.hcaptcha.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

internal class DeferringCaptchaEventsReporter : CaptchaEventsReporter {
    private val _events = MutableStateFlow<List<(CaptchaEventsReporter) -> Unit>>(emptyList())
    val events = _events.asStateFlow()

    override fun init(siteKey: String) {
        enqueue { it.init(siteKey) }
    }

    override fun execute(siteKey: String) {
        enqueue { it.execute(siteKey) }
    }

    override fun success(siteKey: String) {
        enqueue { it.success(siteKey) }
    }

    override fun error(error: Throwable?, siteKey: String) {
        enqueue { it.error(error, siteKey) }
    }

    override fun attachStart() {
        enqueue { it.attachStart() }
    }

    override fun attachEnd(siteKey: String, isReady: Boolean) {
        enqueue { it.attachEnd(siteKey, isReady) }
    }

    fun reportTo(reporter: CaptchaEventsReporter) {
        _events.getAndUpdate { emptyList() }.forEach { it(reporter) }
    }

    private fun enqueue(event: (CaptchaEventsReporter) -> Unit) {
        _events.update { it + event }
    }
}
