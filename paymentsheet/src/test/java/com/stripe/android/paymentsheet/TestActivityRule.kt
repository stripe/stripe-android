package com.stripe.android.paymentsheet

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.R
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

internal inline fun <reified T : Activity> createTestActivityRule(): TestActivityRule<T> {
    return TestActivityRule(T::class.java)
}

internal class TestActivityRule<T : Activity>(
    private val testActivityClass: Class<T>,
) : TestWatcher() {

    private val application: Application
        get() = ApplicationProvider.getApplicationContext()

    private val packageManager: ShadowPackageManager
        get() = shadowOf(application.packageManager)

    private val activityInfo: ActivityInfo
        get() = ActivityInfo().apply {
            name = testActivityClass.name
            packageName = application.packageName
            theme = R.style.StripePaymentSheetDefaultTheme
        }

    private val componentName: ComponentName
        get() = ComponentName(application.packageName, testActivityClass.name)

    override fun starting(description: Description) {
        super.starting(description)
        packageManager.addOrUpdateActivity(activityInfo)
    }

    override fun finished(description: Description) {
        packageManager.removeActivity(componentName)
        super.finished(description)
    }
}
