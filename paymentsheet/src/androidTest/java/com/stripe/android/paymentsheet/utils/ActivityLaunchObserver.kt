package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class ActivityLaunchObserver(
    private val expectedActivityType: Class<out Activity>
) {
    @Volatile
    private var unregisterer: (() -> Unit) = { }

    private val launchedCountDownLatch = CountDownLatch(1)

    fun prepareForLaunch(host: Activity) {
        val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                if (expectedActivityType.isInstance(activity)) {
                    launchedCountDownLatch.countDown()
                }
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        }
        val application = host.applicationContext as Application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        unregisterer = {
            application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        }
    }

    fun awaitLaunch() {
        try {
            if (!launchedCountDownLatch.await(5, TimeUnit.SECONDS)) {
                throw IllegalStateException("Failed to launch.")
            }
        } finally {
            unregisterer()
            unregisterer = { }
        }
    }
}
