package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
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

    internal inline fun <reified T : Activity> createForResult(
        args: ActivityStarter.Args
    ): ActivityScenario<T> {
        return ActivityScenario.launchActivityForResult(
            Intent(context, T::class.java)
                .putExtra(ActivityStarter.Args.EXTRA, args)
        )
    }

    /**
     * Return a view created with an `Activity` context. If you use this, you MUST use
     * [com.stripe.android.utils.TestActivityRule] in your test.
     */
    fun <ViewType : View> createView(
        viewFactory: (Activity) -> ViewType
    ): ViewType {
        var view: ViewType? = null

        ActivityScenario.launch<TestActivity>(
            Intent(context, TestActivity::class.java)
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                view = viewFactory(activity)

                activity.layout.addView(view)
            }
        }

        return requireNotNull(view)
    }

    internal class TestActivity : AppCompatActivity() {
        lateinit var layout: LinearLayout

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            layout = LinearLayout(this)
            setContentView(layout)
        }
    }
}
