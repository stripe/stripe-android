package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import androidx.annotation.CallSuper
import java.util.HashMap
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
abstract class BaseViewTest<T : Activity> protected constructor(private val mClazz: Class<T>) {
    private val mActivityControllers = HashMap<Int, ActivityController<T>>()

    protected fun createActivity(): T {
        val activityController = Robolectric.buildActivity(mClazz)
            .create().start()
            .postCreate(null).resume().visible()
        val activity = activityController.get()
        mActivityControllers[activity.hashCode()] = activityController
        return activityController.get()
    }

    protected fun createActivity(args: ActivityStarter.Args?): T {
        val intent = Intent()
            .putExtra(ActivityStarter.Args.EXTRA, args)
        val activityController = Robolectric.buildActivity(mClazz, intent)
            .create().start()
            .postCreate(null).resume().visible()
        val activity = activityController.get()
        mActivityControllers[activity.hashCode()] = activityController
        return activityController.get()
    }

    protected fun createStartedActivity(): T {
        val activityController = Robolectric.buildActivity(mClazz)
            .create().start()
        val activity = activityController.get()
        mActivityControllers[activity.hashCode()] = activityController
        return activity
    }

    /**
     * Call resume() and visible() on an Activity that was created with
     * [.createStartedActivity].
     */
    protected fun resumeStartedActivity(activity: T) {
        val activityController = mActivityControllers[activity.hashCode()]
        activityController?.resume()?.visible()
    }

    @CallSuper
    @After
    open fun tearDown() {
        for (activityController in mActivityControllers.values) {
            activityController.pause().stop().destroy()
        }
    }
}
