package com.stripe.example.activity

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import org.junit.rules.ExternalResource

class IdlingResourceRule(name: String) : ExternalResource() {
    private val countingResource = CountingIdlingResource(name)

    override fun before() {
        IdlingRegistry.getInstance().register(countingResource)
        BackgroundTaskTracker.onStart = {
            countingResource.increment()
        }
        BackgroundTaskTracker.onStop = {
            countingResource.decrement()
        }
    }

    override fun after() {
        BackgroundTaskTracker.reset()
        IdlingRegistry.getInstance().unregister(countingResource)
    }
}
