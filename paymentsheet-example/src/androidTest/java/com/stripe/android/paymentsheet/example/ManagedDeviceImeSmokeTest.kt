package com.stripe.android.paymentsheet.example

import androidx.activity.compose.setContent
import androidx.compose.material.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that the managed device used for keyboard-dependent tests has a working IME.
 *
 * This test lives in `paymentsheet-example` so the regular PaymentSheet instrumentation workflow
 * does not run it on the `aosp-atd` image, which intentionally omits an input method. The E2E
 * sharder also excludes this class; the dedicated IME workflow is its only CI entry point.
 */
@RunWith(AndroidJUnit4::class)
internal class ManagedDeviceImeSmokeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ManagedDeviceImeTestActivity>()

    @Test
    fun focused_text_field_exposes_a_nonzero_ime_inset() {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                var value by mutableStateOf("")
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag(TEXT_FIELD_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(TEXT_FIELD_TAG).performClick()

        var imeBottomInset = 0
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.activityRule.scenario.onActivity { activity ->
                imeBottomInset = ViewCompat.getRootWindowInsets(
                    activity.window.decorView,
                )?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            imeBottomInset > 0
        }

        assertThat(imeBottomInset).isGreaterThan(0)
    }

    private companion object {
        const val TEXT_FIELD_TAG = "managed_device_ime_smoke_text_field"
    }
}
