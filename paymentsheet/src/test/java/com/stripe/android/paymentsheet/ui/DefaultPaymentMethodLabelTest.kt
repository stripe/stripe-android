package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S])
internal class DefaultPaymentMethodLabelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testDefaultPaymentMethodLabel() {
        composeRule.setContent {
            DefaultPaymentMethodLabel(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        assertThat(composeRule.onRoot().fetchSemanticsNode().children).isNotEmpty()
    }
}