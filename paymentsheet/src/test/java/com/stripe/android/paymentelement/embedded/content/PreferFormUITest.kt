package com.stripe.android.paymentelement.embedded.content

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.ui.FakeAddPaymentMethodInteractor
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
    fun `form displays selected payment method title and icon`() {
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
            PreferFormUI(
                interactor = interactor,
                showFooter = true,
                onMorePaymentMethods = {},
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_HEADER_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_HEADER_ICON).assertIsDisplayed()
    }
}
