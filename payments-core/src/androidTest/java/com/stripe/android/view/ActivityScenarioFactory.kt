package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario

internal class ActivityScenarioFactory(
    private val context: Context
) {

    internal inline fun <reified T : Activity> create(
        args: ActivityStarter.Args? = null
    ): ActivityScenario<T> {
        return ActivityScenario.launch(
            Intent(context, T::class.java).apply {
                if (args != null) {
                    putExtra(ActivityStarter.Args.EXTRA, args)
                }
            }
        )
    }
}
