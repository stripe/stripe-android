package com.stripe.android.ui.core.elements

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.R
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.StaticCardAccountRangeSource
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.elements.events.CardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.CardNumberCompletedEventReporter
import com.stripe.android.ui.core.elements.events.LocalCardBrandDisallowedReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCardBrandFilter
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as StripeUiCoreR
import com.stripe.payments.model.R as PaymentModelR

@RunWith(RobolectricTestRunner::class)
internal class CardNumberControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `When invalid card number verify visible error`() = runTest {
        val cardNumberController = createController()

        cardNumberController.error.test {
            assertThat(awaitItem()).isNull()
            cardNumberController.onValueChange("012")
            assertThat(awaitItem()?.errorMessage).isEqualTo(StripeUiCoreR.string.stripe_blank_and_required)
            assertThat(awaitItem()?.errorMessage).isEqualTo(StripeR.string.stripe_invalid_card_number)
        }
    }

    @Test
    fun `Verify get the form field value correctly`() = runTest {
        val cardNumberController = createController()
        cardNumberController.formFieldValue.test {
            cardNumberController.onValueChange("4242")
            idleLooper()

            assertThat(awaitItem().isComplete)
                .isFalse()
            assertThat(awaitItem().value)
                .isEqualTo("4242")

            cardNumberController.onValueChange("4242424242424242")
            idleLooper()

            assertThat(awaitItem().isComplete)
                .isTrue()
            assertThat(awaitItem().value)
                .isEqualTo("4242424242424242")
        }
    }

    @Test
    fun `Verify error is visible based on the focus`() = runTest {
        val cardNumberController = createController()
        cardNumberController.visibleError.test {
            assertThat(awaitItem()).isFalse()

            cardNumberController.onFocusChange(true)
            cardNumberController.onValueChange("4242")
            expectNoEvents()

            cardNumberController.onFocusChange(false)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `Entering VISA BIN does not call accountRangeRepository`() {
        val fakeRepository = FakeCardAccountRangeRepository()
        val cardNumberController = createController(repository = fakeRepository)

        cardNumberController.onValueChange("42424242424242424242")
        idleLooper()
        assertThat(fakeRepository.numberOfCalls).isEqualTo(0)
    }

    @Test
    fun `Entering valid 19 digit UnionPay BIN returns accountRange of 19`() {
        val cardNumberController = createController()
        cardNumberController.onValueChange("6216828050000000000")
        idleLooper()
        assertThat(cardNumberController.accountRangeService.accountRange!!.panLength).isEqualTo(19)
    }

    @Test
    fun `Entering valid 16 digit UnionPay BIN returns accountRange of 16`() {
        val cardNumberController = createController()
        cardNumberController.onValueChange("6282000000000000")
        idleLooper()
        assertThat(cardNumberController.accountRangeService.accountRange!!.panLength).isEqualTo(16)
    }

    @Test
    fun `trailingIcon should have multi trailing icons when field is empty`() = runTest {
        val cardNumberController = createController()
        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("")
            idleLooper()
            assertThat(awaitItem() as TextFieldIcon.MultiTrailing)
                .isEqualTo(
                    TextFieldIcon.MultiTrailing(
                        staticIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.MasterCard.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.AmericanExpress.icon, isTintable = false)
                        ),
                        animatedIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.Discover.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.JCB.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.DinersClub.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.UnionPay.icon, isTintable = false)
                        )
                    )
                )
        }
    }

    @Test
    fun `trailingIcon should have trailing icon when field matches bin`() = runTest {
        val cardNumberController = createController()

        val multiTrailingIcon = TextFieldIcon.MultiTrailing(
            staticIcons = listOf(
                TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false),
                TextFieldIcon.Trailing(CardBrand.MasterCard.icon, isTintable = false),
                TextFieldIcon.Trailing(CardBrand.AmericanExpress.icon, isTintable = false),
            ),
            animatedIcons = listOf(
                TextFieldIcon.Trailing(CardBrand.Discover.icon, isTintable = false),
                TextFieldIcon.Trailing(CardBrand.JCB.icon, isTintable = false),
                TextFieldIcon.Trailing(CardBrand.DinersClub.icon, isTintable = false),
                TextFieldIcon.Trailing(CardBrand.UnionPay.icon, isTintable = false),
            ),
        )

        val visaIcon = TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false)

        val multiTrailingIconWithJustVisa = TextFieldIcon.MultiTrailing(
            staticIcons = listOf(visaIcon),
            animatedIcons = emptyList(),
        )

        cardNumberController.trailingIcon.test {
            assertThat(awaitItem()).isEqualTo(multiTrailingIcon)

            cardNumberController.onValueChange("4")

            assertThat(awaitItem()).isEqualTo(multiTrailingIconWithJustVisa)
            assertThat(awaitItem()).isEqualTo(visaIcon)

            cardNumberController.onValueChange("")
            assertThat(awaitItem()).isEqualTo(multiTrailingIcon)
        }
    }

    @Test
    fun `trailingIcon should be dropdown if card brand choice eligible`() = runTest {
        val cardNumberController = createController(
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            idleLooper()
            skipItems(2)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = R.string.stripe_card_brand_choice_selection_header.resolvableString,
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.Unknown.code,
                            label = R.string.stripe_card_brand_choice_no_selection.resolvableString,
                            icon = CardBrand.Unknown.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = "Cartes Bancaires".resolvableString,
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = "Visa".resolvableString,
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `trailingIcon should filter out disallowed brands`() = runTest {
        val disallowedBrands = setOf(CardBrand.AmericanExpress, CardBrand.MasterCard)
        val cardBrandFilter = FakeCardBrandFilter(disallowedBrands)
        val cardNumberController = createController(cardBrandFilter = cardBrandFilter)

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("")
            idleLooper()
            assertThat(awaitItem() as TextFieldIcon.MultiTrailing)
                .isEqualTo(
                    TextFieldIcon.MultiTrailing(
                        staticIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.Visa.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.Discover.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.JCB.icon, isTintable = false),
                        ),
                        animatedIcons = listOf(
                            TextFieldIcon.Trailing(CardBrand.DinersClub.icon, isTintable = false),
                            TextFieldIcon.Trailing(CardBrand.UnionPay.icon, isTintable = false)
                        )
                    )
                )
        }
    }

    @Test
    fun `on cbc eligible with preferred brands, should use the preferred brand if none are initially selected`() = runTest {
        val cardNumberController = createController(
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            idleLooper()
            skipItems(3)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = R.string.stripe_card_brand_choice_selection_header.resolvableString,
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = "Cartes Bancaires".resolvableString,
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = "Cartes Bancaires".resolvableString,
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = "Visa".resolvableString,
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on dropdown item click, card brand should have been changed`() = runTest {
        val cardNumberController = createController(
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            cardNumberController.onDropdownItemClicked(
                TextFieldIcon.Dropdown.Item(
                    id = CardBrand.CartesBancaires.code,
                    label = "Cartes Bancaires".resolvableString,
                    icon = PaymentModelR.drawable.stripe_ic_cartes_bancaires
                )
            )
            idleLooper()
            skipItems(3)
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = R.string.stripe_card_brand_choice_selection_header.resolvableString,
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = "Cartes Bancaires".resolvableString,
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = "Cartes Bancaires".resolvableString,
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = "Visa".resolvableString,
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on number updated after update to number with no brands, user choice should be re-used if possible`() = runTest {
        val cardNumberController = createController(
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        cardNumberController.trailingIcon.test {
            cardNumberController.onValueChange("4000002500001001")
            cardNumberController.onDropdownItemClicked(
                TextFieldIcon.Dropdown.Item(
                    id = CardBrand.CartesBancaires.code,
                    label = "Cartes Bancaires".resolvableString,
                    icon = PaymentModelR.drawable.stripe_ic_cartes_bancaires
                )
            )
            cardNumberController.onValueChange("400000250000100")
            skipItems(4)
            cardNumberController.onValueChange("4000002500001001")
            skipItems(1)
            idleLooper()
            assertThat(awaitItem() as TextFieldIcon.Dropdown)
                .isEqualTo(
                    TextFieldIcon.Dropdown(
                        title = R.string.stripe_card_brand_choice_selection_header.resolvableString,
                        currentItem = TextFieldIcon.Dropdown.Item(
                            id = CardBrand.CartesBancaires.code,
                            label = "Cartes Bancaires".resolvableString,
                            icon = CardBrand.CartesBancaires.icon
                        ),
                        items = listOf(
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.CartesBancaires.code,
                                label = "Cartes Bancaires".resolvableString,
                                icon = CardBrand.CartesBancaires.icon
                            ),
                            TextFieldIcon.Dropdown.Item(
                                id = CardBrand.Visa.code,
                                label = "Visa".resolvableString,
                                icon = CardBrand.Visa.icon
                            ),
                        ),
                        hide = false
                    )
                )
        }
    }

    @Test
    fun `on number completed, should report event`() = runTest {
        val eventReporter: CardNumberCompletedEventReporter = mock()

        val cardNumberController = createController(
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                cardNumberController.ComposeUI(
                    enabled = true,
                    field = SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig()
                        ),
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Next,
                    previousFocusDirection = FocusDirection.Next
                )
            }
        }

        cardNumberController.onValueChange("4242424242424242")

        verify(eventReporter, times(1)).onCardNumberCompleted()
    }

    @Test
    fun `on disallowed card brand entered, should report event`() = runTest {
        val fakeDisallowedEventReporter = FakeCardBrandDisallowedReporter()
        val eventReporter: CardNumberCompletedEventReporter = mock()

        val disallowedBrands = setOf(CardBrand.AmericanExpress, CardBrand.MasterCard)
        val cardNumberController = createController(cardBrandFilter = FakeCardBrandFilter(disallowedBrands))

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardBrandDisallowedReporter provides fakeDisallowedEventReporter,
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                cardNumberController.ComposeUI(
                    enabled = true,
                    field = SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig()
                        ),
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Next,
                    previousFocusDirection = FocusDirection.Next,
                )
            }
        }

        fakeDisallowedEventReporter.reportedBrands.test {
            // Simulate entering "37" for American Express
            cardNumberController.onValueChange("37")

            // Expect AmericanExpress to be reported once
            val firstReported = awaitItem()
            assertEquals(CardBrand.AmericanExpress, firstReported, "AmericanExpress should be reported once")

            // Simulate entering "372" (still American Express)
            cardNumberController.onValueChange("372")
            // Simulate clearing the input
            cardNumberController.onValueChange("")
            // Simulate entering "5555" for MasterCard
            expectNoEvents()
            cardNumberController.onValueChange("5555")

            // Expect MasterCard to be reported once
            val secondReported = awaitItem()
            assertEquals(CardBrand.MasterCard, secondReported, "MasterCard should be reported once")

            // Simulate clearing the input and entering an invalid card number
            cardNumberController.onValueChange("")
            cardNumberController.onValueChange("66")
            expectNoEvents()

            // Simulate entering "5555" for MasterCard
            cardNumberController.onValueChange("5555")

            // Expect MasterCard to be reported once
            val thirdReported = awaitItem()
            assertEquals(CardBrand.MasterCard, thirdReported, "MasterCard should be reported once")

            // Simulate entering a valid Visa card number
            cardNumberController.onValueChange("4242424242424242")
            expectNoEvents()

            // Simulate entering a MasterCard
            cardNumberController.onValueChange("")
            cardNumberController.onValueChange("5555555555554444")

            // Expect MasterCard to be reported once
            val fourthReported = awaitItem()
            assertEquals(CardBrand.MasterCard, fourthReported, "MasterCard should be reported once")
        }
    }

    @Test
    fun `on initial number completed, should not report event`() = runTest {
        val eventReporter: CardNumberCompletedEventReporter = mock()

        val cardNumberController = createController(
            initialValue = "4242424242424242",
            cardBrandChoiceConfig = CardBrandChoiceConfig.Eligible(
                preferredBrands = listOf(CardBrand.CartesBancaires),
                initialBrand = null
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides eventReporter
            ) {
                cardNumberController.ComposeUI(
                    enabled = true,
                    field = SimpleTextElement(
                        identifier = IdentifierSpec.Name,
                        controller = SimpleTextFieldController(
                            textFieldConfig = SimpleTextFieldConfig()
                        ),
                    ),
                    modifier = Modifier.testTag(TEST_TAG),
                    hiddenIdentifiers = emptySet(),
                    lastTextFieldIdentifier = null,
                    nextFocusDirection = FocusDirection.Next,
                    previousFocusDirection = FocusDirection.Next
                )
            }
        }

        verifyNoInteractions(eventReporter)
    }

    @Test
    fun `determineSelectedBrand - single non-blocked brand picks that brand`() = runTest {
        // Disallow Visa
        val disallowedBrands = setOf(CardBrand.Visa)
        val filter = FakeCardBrandFilter(disallowedBrands)
        val allChoices = listOf(CardBrand.Visa, CardBrand.MasterCard)
        val previous = CardBrand.Visa // The user had picked Visa before

        // Because exactly one brand (MasterCard) is allowed and there are multiple total choices,
        // the function should pick MasterCard.
        val result = createController().determineSelectedBrand(
            previous = previous,
            allChoices = allChoices,
            cardBrandFilter = filter,
            preferredBrands = emptyList()
        )
        assertThat(result).isEqualTo(CardBrand.MasterCard)
    }

    @Test
    fun `determineSelectedBrand - previous is in allChoices returns previous`() = runTest {
        val allChoices = listOf(CardBrand.Visa, CardBrand.MasterCard)
        val previous = CardBrand.Visa

        // Because `previous` is already in `allChoices`,
        // the function should return `previous` (Visa) unchanged.
        val result = createController().determineSelectedBrand(
            previous = previous,
            allChoices = allChoices,
            cardBrandFilter = DefaultCardBrandFilter,
            preferredBrands = emptyList()
        )
        assertThat(result).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun `determineSelectedBrand - previous is Unknown returns Unknown`() = runTest {
        val allChoices = listOf(CardBrand.Visa, CardBrand.MasterCard)
        val previous = CardBrand.Unknown

        // Because previous is Unknown, the function returns Unknown.
        val result = createController().determineSelectedBrand(
            previous = previous,
            allChoices = allChoices,
            cardBrandFilter = DefaultCardBrandFilter,
            preferredBrands = emptyList()
        )
        assertThat(result).isEqualTo(CardBrand.Unknown)
    }

    @Test
    fun `determineSelectedBrand - previous not in choices falls back to first available preferred`() = runTest {
        val allChoices = listOf(CardBrand.Visa, CardBrand.MasterCard)
        val previous = CardBrand.AmericanExpress
        // Visa is a preferred network over CB
        val preferredBrands = listOf(CardBrand.Visa, CardBrand.CartesBancaires)

        // Because previous is not in allChoices, we fall back to the first available in `preferredBrands`.
        val result = createController().determineSelectedBrand(
            previous = previous,
            allChoices = allChoices,
            cardBrandFilter = DefaultCardBrandFilter,
            preferredBrands = preferredBrands
        )
        assertThat(result).isEqualTo(CardBrand.Visa)
    }

    @Test
    fun `determineSelectedBrand - no valid preferred brand defaults to Unknown`() = runTest {
        val allChoices = listOf(CardBrand.Visa, CardBrand.MasterCard)
        val previous = CardBrand.AmericanExpress
        // None of these are in allChoices
        val preferredBrands = listOf(CardBrand.CartesBancaires, CardBrand.UnionPay)

        // Because previous is not in allChoices and none of the preferred brands are available,
        // the function should return Unknown.
        val result = createController().determineSelectedBrand(
            previous = previous,
            allChoices = allChoices,
            cardBrandFilter = DefaultCardBrandFilter,
            preferredBrands = preferredBrands
        )
        assertThat(result).isEqualTo(CardBrand.Unknown)
    }

    private fun createController(
        initialValue: String? = null,
        cardBrandChoiceConfig: CardBrandChoiceConfig = CardBrandChoiceConfig.Ineligible,
        repository: CardAccountRangeRepository = FakeCardAccountRangeRepository(),
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter
    ): DefaultCardNumberController {
        return DefaultCardNumberController(
            cardTextFieldConfig = CardNumberConfig(
                isCardBrandChoiceEligible = false,
                cardBrandFilter = cardBrandFilter
            ),
            cardAccountRangeRepository = repository,
            uiContext = testDispatcher,
            workContext = testDispatcher,
            initialValue = initialValue,
            cardBrandChoiceConfig = cardBrandChoiceConfig,
            cardBrandFilter = cardBrandFilter
        )
    }

    private class FakeCardAccountRangeRepository : CardAccountRangeRepository {

        private val staticCardAccountRangeSource = StaticCardAccountRangeSource()

        var numberOfCalls: Int = 0
            private set

        override suspend fun getAccountRange(
            cardNumber: CardNumber.Unvalidated
        ): AccountRange? {
            numberOfCalls += 1
            return cardNumber.bin?.let {
                staticCardAccountRangeSource.getAccountRange(cardNumber)
            }
        }

        override suspend fun getAccountRanges(
            cardNumber: CardNumber.Unvalidated
        ): List<AccountRange>? {
            numberOfCalls += 1
            return cardNumber.bin?.let {
                staticCardAccountRangeSource.getAccountRanges(cardNumber)
            }
        }

        override val loading: StateFlow<Boolean> = stateFlowOf(false)
    }

    private companion object {
        const val TEST_TAG = "CardNumberElement"
    }
}

class FakeCardBrandDisallowedReporter : CardBrandDisallowedReporter {
    private val _reportedBrands = MutableSharedFlow<CardBrand>(extraBufferCapacity = Int.MAX_VALUE)
    val reportedBrands = _reportedBrands.asSharedFlow()

    override fun onDisallowedCardBrandEntered(brand: CardBrand) {
        _reportedBrands.tryEmit(brand)
    }
}
