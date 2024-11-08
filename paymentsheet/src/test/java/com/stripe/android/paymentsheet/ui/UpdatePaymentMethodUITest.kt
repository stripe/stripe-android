package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class UpdatePaymentMethodUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingExpiryDate_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = null,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = -1,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryYear_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryYear = 202,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun singleDigitExpiryMonth_hasLeadingZero() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = 8,
                expiryYear = 2029,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "08/29"
            )
        }
    }

    @Test
    fun doubleDigitExpiryMonth_doesNotHaveLeadingZero() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = 11,
                expiryYear = 2029,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "11/29"
            )
        }
    }

    @Test
    fun threeDigitCvcCardBrand_displaysThreeDotsForCvc() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                brand = CardBrand.Visa
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertCvcEquals(
                "•••"
            )
        }
    }

    @Test
    fun fourDigitCvcCardBrand_displaysFourDotsForCvc() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                brand = CardBrand.AmericanExpress
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertCvcEquals(
                "••••"
            )
        }
    }

    private fun assertExpiryDateEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private fun assertCvcEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_CVC_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private fun runScenario(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        testBlock: () -> Unit,
    ) {
        val interactor = DefaultUpdatePaymentMethodInteractor(
            isLiveMode = false,
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            card = displayableSavedPaymentMethod.paymentMethod.card!!,
        )

        composeRule.setContent {
            UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
        }

        testBlock()
    }
}
