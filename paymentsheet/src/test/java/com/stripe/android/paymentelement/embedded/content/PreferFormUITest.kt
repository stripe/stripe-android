package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentElementTheme
import com.stripe.android.paymentsheet.verticalmode.DisplayablePaymentMethod
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_ICON
import com.stripe.android.paymentsheet.verticalmode.TEST_TAG_HEADER_TITLE
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class PreferFormUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `form displays persistent Payment heading without icon`() {
        val metadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "affirm"),
            ),
        )
        val interactor = FakeAddPaymentMethodInteractor(
            initialState = FakeAddPaymentMethodInteractor.createState(
                metadata = metadata,
                paymentMethodCode = "affirm",
            ),
        )

        composeRule.setContent {
            PaymentElementTheme(appearance = PaymentSheet.Appearance()) {
                PreferFormHeaderUI(enabled = true)
                PreferFormUI(
                    interactor = interactor,
                    showFooter = true,
                    paymentMethodCount = 2,
                    onMorePaymentMethods = {},
                )
            }
        }

        composeRule.onNodeWithTag(TEST_TAG_HEADER_TITLE)
            .assertIsDisplayed()
            .assertTextEquals("Payment")
        composeRule.onNodeWithTag(TEST_TAG_HEADER_ICON).assertDoesNotExist()
    }

    @Test
    fun `redirect confirmation displays web redirect copy`() {
        composeRule.setContent {
            PaymentElementTheme(appearance = PaymentSheet.Appearance()) {
                PreferFormRedirectConfirmation(
                    paymentMethod = DisplayablePaymentMethod(
                        code = "crypto",
                        displayName = "Crypto".resolvableString,
                        iconResource = 0,
                        iconResourceNight = null,
                        lightThemeIconUrl = null,
                        darkThemeIconUrl = null,
                        iconRequiresTinting = true,
                        onClick = {},
                    )
                )
            }
        }

        composeRule.onNodeWithTag(PREFER_FORM_REDIRECT_CONFIRMATION_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Crypto").assertIsDisplayed()
        composeRule.onNodeWithText(
            "After submission, you will be redirected to securely complete next steps."
        ).assertIsDisplayed()
    }
}
