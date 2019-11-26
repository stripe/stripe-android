package com.stripe.android.view

import android.app.Activity
import androidx.annotation.CallSuper
import java.util.HashMap
import kotlin.test.AfterTest
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
internal abstract class BaseViewTest<T : Activity> protected constructor(
    private val activityClass: Class<T>
) {
    private val activityControllers = HashMap<Int, ActivityController<T>>()

    protected fun createStartedActivity(): T {
        val activityController = Robolectric.buildActivity(activityClass)
            .create().start()
        val activity = activityController.get()
        activityControllers[activity.hashCode()] = activityController
        return activity
    }

    /**
     * Call resume() and visible() on an Activity that was created with [createStartedActivity].
     */
    protected fun resumeStartedActivity(activity: T) {
        activityControllers[activity.hashCode()]?.resume()?.visible()
    }

    @CallSuper
    @AfterTest
    open fun tearDown() {
        activityControllers.values.forEach {
            it.pause().stop().destroy()
        }
    }
}
