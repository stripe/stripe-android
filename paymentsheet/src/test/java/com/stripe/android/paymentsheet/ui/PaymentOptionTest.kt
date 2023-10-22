package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.PaymentOptionUi
import com.stripe.android.paymentsheet.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentOptionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Turns card label into screen reader-friendly text`() {
        val label = "Card ending in 4242"

        composeTestRule.setContent {
            PaymentOptionUi(
                viewWidth = 100.dp,
                isSelected = false,
                editState = PaymentOptionEditState.None,
                isEnabled = true,
                iconRes = R.drawable.stripe_ic_paymentsheet_card_visa,
                description = label,
                labelText = label,
                onItemSelectedListener = {},
            )
        }

        composeTestRule
            .onNodeWithText(label)
            .onParent()
            .assertContentDescriptionEquals("Card ending in 4 2 4 2 ")
    }
}
