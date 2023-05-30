package com.stripe.android.link

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule

/**
 * Gets the activity from a scenarioRule.
 *
 * https://androidx.tech/artifacts/compose.ui/ui-test-junit4/1.0.0-alpha11-source/androidx/compose/ui/test/junit4/AndroidComposeTestRule.kt.html
 */
fun <A : ComponentActivity> ActivityScenarioRule<A>.getActivity(): A {
    var activity: A? = null

    scenario.onActivity { activity = it }

    return activity
        ?: throw IllegalStateException("Activity was not set in the ActivityScenarioRule!")
}
