package com.stripe.android.stripe3ds2.views

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.utils.CustomizeUtils
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class HeaderZoneCustomizerTest {
    private val activityScenarioFactory = ActivityScenarioFactory(
        ApplicationProvider.getApplicationContext()
    )

    @Test
    fun customize_nullCustomizations_defaultValuesSet() {
        createActivity { activity ->
            val cancelButton = HeaderZoneCustomizer(activity)
                .customize()

            assertEquals(
                "Secure Checkout",
                activity.supportActionBar?.title.toString()
            )

            assertEquals("Cancel", cancelButton?.text.toString())
        }
    }

    @Test
    fun customize_toolbarCustomization_actionBarCustomized() {
        createActivity { activity ->
            val toolbarCustomization = StripeToolbarCustomization().also {
                it.setBackgroundColor("#000000")
                it.setStatusBarColor("#FF0000")
                it.setTextColor("#FFFFFF")
                it.setHeaderText("HEADER")
                it.setButtonText("BUTTON")
                it.textFontSize = 16
            }

            val cancelButton = HeaderZoneCustomizer(activity)
                .customize(toolbarCustomization = toolbarCustomization)

            val actionBar = activity.supportActionBar
            assertEquals("HEADER", actionBar?.title.toString())
            assertEquals("BUTTON", cancelButton?.text.toString())

            assertEquals(Color.RED, activity.window.statusBarColor)
        }
    }

    @Test
    fun customizeStatusBar_backgroundColor_darkenedBackgroundColor() {
        createActivity { activity ->
            val toolbarCustomization = StripeToolbarCustomization().also {
                it.setBackgroundColor("#FF0000")
            }

            HeaderZoneCustomizer.customizeStatusBar(activity, toolbarCustomization)

            assertEquals(
                CustomizeUtils.darken(Color.RED),
                activity.window.statusBarColor
            )
        }
    }

    private fun createActivity(callback: (AppCompatActivity) -> Unit) {
        activityScenarioFactory.create().use {
            it.onActivity(callback)
        }
    }
}
