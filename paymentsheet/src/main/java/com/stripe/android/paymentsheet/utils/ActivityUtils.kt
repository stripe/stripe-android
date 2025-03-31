package com.stripe.android.paymentsheet.utils

import android.app.Activity
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE

internal fun Activity.applicationIsTaskOwner(): Boolean {
    val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

    /*
     * `appTasks` is populated with tasks that the application owns. This means that if another application launched
     * the application wrapping the SDK, the task will not found in the list of application tasks owned by the
     * application.
     *
     * `launchMode` affects this list as well. If a `launchMode` that starts its own task with the activity as the
     * base activity, then the application wrapping the SDK would own the task and it would show up in the list of
     * application tasks.
     *
     * With this information, we can figure out if the application the activity belongs to is the owner of the task
     * itself by using the activity's task id
     */
    return activityManager.appTasks.any { task ->
        @Suppress("DEPRECATION")
        task.taskInfo.persistentId == taskId
    }
}
