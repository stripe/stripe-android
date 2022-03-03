package com.stripe.android.paymentsheet

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalAnimationApi
@RunWith(AndroidJUnit4::class)
internal class PaymentMethodsUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun test() {
        val paymentMethods = listOf(
            SupportedPaymentMethod.Bancontact,
            SupportedPaymentMethod.Sofort,
            SupportedPaymentMethod.AfterpayClearpay,
            SupportedPaymentMethod.Eps
        )
        composeTestRule.setContent {
            PaymentMethodsUI(
                paymentMethods = paymentMethods,
                selectedIndex = 0,
                isEnabled = true,
                onItemSelectedListener = {}
            )
        }

        // Expect the value to be equal to the width of the screen
        // minus the left and right padding for each card
        // calculateViewWidth(
        //     composeTestRule.activity.resources.displayMetrics,
        //     paymentMethods.size
        // ) - (CARD_HORIZONTAL_PADDING.dp * 2)
        composeTestRule.onNodeWithTag(TEST_TAG_LIST + "Bancontact")
            .assertWidthIsEqualTo(
                109.454544.dp
            )
    }
}
