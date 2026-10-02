package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.SupportedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class PreferFormFooterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `footer displays no more than three alternative icons`() {
        setContent(enabled = true, alternatives = List(4, ::paymentMethod), paymentMethodCount = 4)

        composeRule.onAllNodesWithTag(
            PREFER_FORM_FOOTER_ICON_TEST_TAG,
            useUnmergedTree = true,
        ).assertCountEquals(3)
    }

    @Test
    fun `footer displays label and total payment method count`() {
        setContent(
            enabled = true,
            alternatives = List(3, ::paymentMethod),
            paymentMethodCount = 5,
        )

        composeRule.onNodeWithText("Pay another way").assertIsDisplayed()
        composeRule.onNodeWithTag(
            PREFER_FORM_FOOTER_COUNT_TEST_TAG,
            useUnmergedTree = true,
        ).assertTextEquals("+5")
    }

    @Test
    fun `enabled footer invokes click`() {
        val clicks = ViewActionRecorder<Unit>()
        setContent(enabled = true, alternatives = listOf(paymentMethod(0)), paymentMethodCount = 1) {
            clicks.record(Unit)
        }

        composeRule.onNodeWithTag(PREFER_FORM_FOOTER_TEST_TAG).assertIsEnabled().performClick()

        clicks.consume(Unit)
    }

    @Test
    fun `processing disables footer`() {
        setContent(enabled = false, alternatives = listOf(paymentMethod(0)), paymentMethodCount = 1)

        composeRule.onNodeWithTag(PREFER_FORM_FOOTER_TEST_TAG).assertIsNotEnabled()
    }

    private fun setContent(
        enabled: Boolean,
        alternatives: List<SupportedPaymentMethod>,
        paymentMethodCount: Int,
        onClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            PaymentElementTheme(appearance = PaymentSheet.Appearance()) {
                PreferFormFooter(
                    alternatives = alternatives,
                    paymentMethodCount = paymentMethodCount,
                    enabled = enabled,
                    onClick = onClick,
                )
            }
        }
    }

    private fun paymentMethod(index: Int): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = "method_$index",
            displayName = "Method $index".resolvableString,
            iconResource = 0,
            iconResourceNight = null,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
            iconRequiresTinting = false,
        )
    }
}
