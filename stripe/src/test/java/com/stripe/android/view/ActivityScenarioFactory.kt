package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario

internal class ActivityScenarioFactory(
    private val context: Context
) {
    inline fun <reified T : Activity> create(
        args: ActivityStarter.Args
    ): ActivityScenario<T> {
        return ActivityScenario.launch(
            Intent(context, T::class.java)
                .putExtra(ActivityStarter.Args.EXTRA, args)
        )
    }
}
