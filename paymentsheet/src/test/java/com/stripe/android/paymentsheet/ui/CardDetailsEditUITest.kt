package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.TEST_TAG_DROP_DOWN_CHOICE
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun missingExpiryMonth_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = null
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun missingExpiryYear_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = null
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_below1_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 0
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_above12_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 13
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryYear_below2000_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = 1999
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryYear_above2100_displaysDots_whenExpDateIsReadOnly() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = 2101
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_below1_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 0
            ),
            expiryDateEditEnabled = false
        ) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_over12_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 202
            ),
        ) {
            assertExpiryDateEquals(
                "00 / 29"
            )
        }
    }

    @Test
    fun invalidExpiryYear_below2000_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = 1999
            ),
        ) {
            assertExpiryDateEquals(
                "08 / 00"
            )
        }
    }

    @Test
    fun invalidExpiryYear_above2100_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = 2101
            ),
        ) {
            assertExpiryDateEquals(
                "08 / 00"
            )
        }
    }

    @Test
    fun missingExpiryMonth_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = null
            ),
        ) {
            assertExpiryDateEquals(
                "00 / 29"
            )
        }
    }

    @Test
    fun missingExpiryYear_displaysDots_whenExpDateIsEditable() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryYear = null
            ),
        ) {
            assertExpiryDateEquals(
                "08 / 00"
            )
        }
    }

    @Test
    fun expiryDateFieldDisabled_whenExpDateIsReadOnly() {
        runScenario(
            expiryDateEditEnabled = false
        ) {
            composeRule.onNodeWithTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG).assertIsNotEnabled()
        }
    }

    @Test
    fun singleDigitExpiryMonth_hasLeadingZero() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(
                expiryMonth = 8,
                expiryYear = 2029,
            ),
        ) {
            assertExpiryDateEquals(
                "08 / 29"
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
                "11 / 29"
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
    fun showCardBrandDropdownIsFalse_cbcDropdownIsNotShown() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
            showCardBrandDropdown = false
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun selectingCardBrandDropdown_sendsOnBrandChoiceChangedAction() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG).performClick()

            composeRule.onNodeWithTag("${TEST_TAG_DROP_DOWN_CHOICE}_Visa").performClick()

            viewActionRecorder.consume(
                viewAction = EditCardDetailsInteractor.ViewAction.BrandChoiceChanged(
                    cardBrandChoice = CardBrandChoice(
                        brand = CardBrand.Visa,
                        enabled = true
                    )
                )
            )
        }
    }

    @Test
    fun validExpiryDateInput_textIsCorrectlyFormatted() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        ) {
            performExpiryDateInput("1255")

            assertExpiryDateEquals("12 / 55")
            composeRule.onNodeWithTag(CARD_EDIT_UI_ERROR_MESSAGE).assertDoesNotExist()
        }
    }

    @Test
    fun incompleteExpiryDateInput_textIsCorrectlyFormatted_errorMessageIsDisplayed() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        ) {
            performExpiryDateInput("12")

            assertExpiryDateEquals("12 / ")
            composeRule.onNodeWithTag(CARD_EDIT_UI_ERROR_MESSAGE).assertDoesNotExist()
        }
    }

    @Test
    fun invalidExpiryDateInput_textIsCorrectlyFormatted_errorMessageIsDisplayed() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        ) {
            performExpiryDateInput("1221")

            assertExpiryDateEquals("12 / 21")
            composeRule.onNodeWithTag(CARD_EDIT_UI_ERROR_MESSAGE)
                .assert(hasText("Your card's expiration year is invalid."))
        }
    }

    @Test
    fun expiryDateInput_invokesCorrectViwAction() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        ) {
            performExpiryDateInput("1229")

            viewActionRecorder.consume(
                viewAction = EditCardDetailsInteractor.ViewAction.DateChanged("1229")
            )
        }
    }

    @Test
    fun `Card drop down has accessibility label`() {
        runScenario(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS
        ) {
            composeRule.onNodeWithTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
                .assertContentDescriptionEquals("Cartes Bancaires")
        }
    }

    private fun assertExpiryDateEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private fun performExpiryDateInput(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG).performTextReplacement(text)
    }

    private fun assertCvcEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_CVC_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private fun runScenario(
        card: PaymentMethod.Card = PaymentMethodFixtures.CARD_WITH_NETWORKS,
        showCardBrandDropdown: Boolean = true,
        expiryDateEditEnabled: Boolean = true,
        block: TestScenario.() -> Unit
    ) {
        val editCardDetailsInteractor = FakeEditCardDetailsInteractor(
            card = card,
            shouldShowCardBrandDropdown = showCardBrandDropdown,
            expiryDateEditEnabled = expiryDateEditEnabled,
        )
        composeRule.setContent {
            CardDetailsEditUI(
                editCardDetailsInteractor = editCardDetailsInteractor,
            )
        }
        block(TestScenario(editCardDetailsInteractor.viewActionRecorder))
    }

    data class TestScenario(
        val viewActionRecorder: ViewActionRecorder<EditCardDetailsInteractor.ViewAction>
    )
}
