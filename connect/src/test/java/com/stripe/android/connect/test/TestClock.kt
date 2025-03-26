package com.stripe.android.connect.test

import com.stripe.android.connect.util.Clock

class TestClock(var millis: Long = 0L) : Clock {
    override fun millis(): Long = millis
}
