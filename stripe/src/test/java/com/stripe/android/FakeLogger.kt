package com.stripe.android

internal class FakeLogger : Logger {
    override fun debug(msg: String) {
    }

    override fun error(msg: String, t: Throwable?) {
    }

    override fun info(msg: String) {
    }
}
