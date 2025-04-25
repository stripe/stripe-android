package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.elements.Mandate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
internal class MandateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun testMandateText() {
        composeRule.setContent {
            Mandate(
                mandateText = "Accept me!",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        assertThat(composeRule.onRoot().fetchSemanticsNode().children).isNotEmpty()
    }

    @Test
    fun testNullMandateText() {
        composeRule.setContent {
            Mandate(
                mandateText = null,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        assertThat(composeRule.onRoot().fetchSemanticsNode().children).isEmpty()
    }
}
