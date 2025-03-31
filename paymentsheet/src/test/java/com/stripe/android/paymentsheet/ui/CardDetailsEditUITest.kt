package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class CardDetailsEditUITest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingExpiryDate_displaysDots() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = null
            )
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_displaysDots() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = -1
            )
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryYear_displaysDots() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 202
            )
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun singleDigitExpiryMonth_hasLeadingZero() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 8,
                expiryYear = 2029,
            )
        ) {
            assertExpiryDateEquals(
                "08/29"
            )
        }
    }

    @Test
    fun doubleDigitExpiryMonth_doesNotHaveLeadingZero() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 11,
                expiryYear = 2029,
            )
        ) {
            assertExpiryDateEquals(
                "11/29"
            )
        }
    }

    @Test
    fun threeDigitCvcCardBrand_displaysThreeDotsForCvc() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                brand = CardBrand.Visa,
            )
        ) {
            assertCvcEquals(
                "•••"
            )
        }
    }

    @Test
    fun fourDigitCvcCardBrand_displaysFourDotsForCvc() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                brand = CardBrand.AmericanExpress,
            )
        ) {
            assertCvcEquals(
                "••••"
            )
        }
    }

    @Test
    fun modifiableCard_cbcDropdownIsShown() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).assertExists()
        }
    }

    @Test
    fun notModifiableCard_cbcDropdownIsNotShown() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun selectingCardBrandDropdown_sendsOnBrandChoiceChangedAction() {
        var brandChange: CardBrand? = null
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
            onBrandChoiceChanged = {
                brandChange = it.brand
            }
        )

        composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).performClick()

        composeRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_Visa").performClick()

        assertThat(brandChange).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun `Card drop down has accessibility label`() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
                .assertContentDescriptionEquals("Visa")
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
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        selectedCardBrand: CardBrandChoice = CardBrandChoice(CardBrand.Visa, enabled = true),
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        showCardBrandDropdown: Boolean = true,
        isExpiredCard: Boolean = false,
        onBrandChoiceChanged: (CardBrandChoice) -> Unit = {},
    ) {
        composeRule.setContent {
            CardDetailsEditUI(
                shouldShowCardBrandDropdown = showCardBrandDropdown,
                selectedBrand = selectedCardBrand,
                card = card,
                isExpired = isExpiredCard,
                cardBrandFilter = cardBrandFilter,
                paymentMethodIcon = com.stripe.payments.model.R.drawable.stripe_ic_visa_unpadded,
                onBrandChoiceChanged = onBrandChoiceChanged,
            )
        }
    }
}
