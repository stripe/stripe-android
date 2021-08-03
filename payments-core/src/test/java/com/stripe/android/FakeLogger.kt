package com.stripe.android

internal class FakeLogger : Logger {
    val debugLogs = mutableListOf<String>()
    val infoLogs = mutableListOf<String>()
    val warningLogs = mutableListOf<String>()
    val errorLogs = mutableListOf<Pair<String, Throwable?>>()

    override fun debug(msg: String) {
        debugLogs.add(msg)
    }

    override fun info(msg: String) {
        infoLogs.add(msg)
    }

    override fun warning(msg: String) {
        warningLogs.add(msg)
    }

    override fun error(msg: String, t: Throwable?) {
        errorLogs.add(msg to t)
    }
}
