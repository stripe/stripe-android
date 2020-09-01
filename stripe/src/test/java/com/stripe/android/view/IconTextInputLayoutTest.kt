package com.stripe.android.view

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputLayout
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Test class for [IconTextInputLayout] to ensure that the Reflection doesn't break
 * during an upgrade. This class exists only to wrap [TextInputLayout], so there
 * is no need to otherwise test the behavior.
 */
@RunWith(RobolectricTestRunner::class)
internal class IconTextInputLayoutTest {

    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun init_successfullyFindsFields() {
        val context: Context = ApplicationProvider.getApplicationContext()
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

        activityScenarioFactory
            .createAddPaymentMethodActivity()
            .use { activityScenario ->
                activityScenario.onActivity {
                    val iconTextInputLayout = it
                        .findViewById<CardMultilineWidget>(R.id.card_multiline_widget)
                        .findViewById<IconTextInputLayout>(R.id.tl_card_number)
                    assertTrue(iconTextInputLayout.hasObtainedCollapsingTextHelper())
                }
            }
    }
}
