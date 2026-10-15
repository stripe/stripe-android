package com.stripe.android.utils

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class NetworkResponseGate {
    val requestReceived = CountDownLatch(1)
    val releaseResponse = CountDownLatch(1)

    fun awaitRelease() {
        requestReceived.countDown()
        check(releaseResponse.await(10, TimeUnit.SECONDS))
    }
}
