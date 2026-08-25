package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.model.AccountRange
import com.stripe.android.model.CardBrand
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.events.CardNumberCompletedEventReporter
import com.stripe.android.ui.core.elements.events.LocalCardNumberCompletedEventReporter
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.FieldValidationMessageComparator
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.RowElement
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.isInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsControllerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    private val coroutineScope = CoroutineScope(testDispatcher)

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Validation message uses comparator to determine which message to show`() = runTest {
        val cardDetailsTextFieldConfig = FakeCardNumberTextFieldConfig(
            defaultCardNumberTextFieldConfig = CardNumberConfig(
                isCardBrandChoiceEligible = false,
                cardBrandFilter = DefaultCardBrandFilter
            ),
            textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Warning(0)
            )
        )
        val cvcTextFieldConfig = FakeCvcTextFieldConfig(
            defaultCvcTextFieldConfig = CvcConfig(),
            textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Warning(1)
            )
        )
        val expiryDateConfig = FakeTextFieldConfig(
            defaultTextFieldConfig = DateConfig(),
            textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Error(2)
            )
        )
        val cardController = cardDetailsController(
            cardDetailsTextFieldConfig = cardDetailsTextFieldConfig,
            cvcTextFieldConfig = cvcTextFieldConfig,
            dateConfig = expiryDateConfig
        )

        // Fake FieldValidationMessageComparator sorts by message ID ascending
        cardController.validationMessage.test {
            assertThat(awaitItem()?.message).isEqualTo(0)

            cardDetailsTextFieldConfig.textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Error(5)
            )
            cvcTextFieldConfig.textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Warning(-4)
            )
            expiryDateConfig.textFieldState = TextFieldStateConstants.Error.Invalid(
                validationMessage = FieldValidationMessage.Warning(-5)
            )
            cardController.numberElement.controller.onValueChange("4242424242424244")
            cardController.cvcElement.controller.onValueChange("124")
            cardController.expirationDateElement.controller.onValueChange("13")
            idleLooper()

            // Verify the validation message changed (comparator re-sorted)
            assertThat(awaitItem()?.message).isEqualTo(1)
            assertThat(awaitItem()?.message).isEqualTo(-4)
            assertThat(awaitItem()?.message).isEqualTo(-5)
        }
    }

    @Test
    fun `When eligible for card brand choice and preferred card brand is passed, initial value should have been set`() =
        runTest {
            val cardController = cardDetailsController(
                initialValues = mapOf(
                    IdentifierSpec.CardNumber to "4000002500001001",
                    IdentifierSpec.PreferredCardBrand to CardBrand.CartesBancaires.code
                ),
                cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf())
            )

            cardController.numberElement.controller.cardBrandFlow.test {
                assertThat(awaitItem()).isEqualTo(CardBrand.CartesBancaires)
            }
        }

    @Test
    fun `When new card scanned with no existing card, fields properly filled in`() = runTest {
        val cardController = cardDetailsController()
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")

        idleLooper()

        cardController.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = 2044,
                expirationMonth = 4,
            )
        )

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("0444")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When new card overwrites existing card, fields properly filled in`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpYear to "2042",
                IdentifierSpec.CardExpMonth to "2",
                IdentifierSpec.CardCvc to "123",
            )
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("242")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("123")

        idleLooper()

        cardController.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = 2044,
                expirationMonth = 4,
            )
        )

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("0444")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When new card scanned with invalid expiry date, should not use invalid date`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpYear to "2042",
                IdentifierSpec.CardExpMonth to "2",
                IdentifierSpec.CardCvc to "123",
            )
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("242")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("123")

        idleLooper()

        cardController.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = 2009,
                expirationMonth = 12,
            )
        )

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When new card scanned with single digit month, date is correctly formatted`() = runTest {
        val cardController = cardDetailsController()

        idleLooper()

        cardController.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = 2029,
                expirationMonth = 1,
            )
        )

        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("0129")
    }

    @Test
    fun `When initialized with validated scan and card number, card pill is shown`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardValidatedScan to "true",
                IdentifierSpec.CardExpMonth to "06",
                IdentifierSpec.CardExpYear to "2030",
            )
        )

        idleLooper()

        cardController.fields.test {
            val fields = awaitItem()

            assertThat(fields).hasSize(2)

            assertThat(fields[0]).isInstanceOf<CardPillElement>()
            assertThat(fields[1]).isSameInstanceAs(cardController.cvcElement)

            ensureAllEventsConsumed()
        }

        assertThat(cardController.cardPillElement.value).isNotNull()
    }

    @Test
    fun `When initialized with validated scan but no card number, card pill is not shown`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardValidatedScan to "true",
            )
        )
        idleLooper()

        cardController.fields.test {
            val fields = awaitItem()

            assertThat(fields).hasSize(2)

            assertThat(fields[0]).isSameInstanceAs(cardController.numberElement)
            assertThat(fields[1]).isInstanceOf(RowElement::class.java)

            ensureAllEventsConsumed()
        }

        assertThat(cardController.cardPillElement.value).isNull()
    }

    @Test
    fun `When initialized with validated scan false, card pill is not shown`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardValidatedScan to "false",
            )
        )
        idleLooper()

        cardController.fields.test {
            val fields = awaitItem()

            assertThat(fields).hasSize(2)
            assertThat(fields[0]).isSameInstanceAs(cardController.numberElement)
            assertThat(fields[1]).isInstanceOf(RowElement::class.java)

            ensureAllEventsConsumed()
        }

        assertThat(cardController.cardPillElement.value).isNull()
    }

    @Test
    fun `When validated scanned card, card data is applied and fields have card pill & cvc`() = runTest {
        val cardController = cardDetailsController()
        idleLooper()

        cardController.fields.test {
            val before = awaitItem()
            assertThat(before).hasSize(2)
            assertThat(before[0]).isSameInstanceAs(cardController.numberElement)
            assertThat(before[1]).isInstanceOf(RowElement::class.java)

            cardController.onScannedCard(
                ScannedCardDetails.Validated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2030,
                    expirationMonth = 6,
                )
            )
            idleLooper()

            val after = awaitItem()
            assertThat(after).hasSize(2)
            assertThat(after[0]).isInstanceOf<CardPillElement>()

            val cardPillElement = after[0] as CardPillElement

            assertThat(cardPillElement.controller.cardNumber).isEqualTo("4242424242424242")
            assertThat(after[1]).isSameInstanceAs(cardController.cvcElement)
            ensureAllEventsConsumed()
        }

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("0630")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value).isEqualTo("")
    }

    @Test
    fun `When card pill is dismissed, pill is hidden and fields are cleared`() = composeTest { controller ->
        turbineScope {
            val cardPillTurbine = controller.cardPillElement.testIn(this)
            val numberControllerValueTurbine =
                controller.numberElement.controller.rawFieldValue.testIn(this)
            val expirationControllerValueTurbine =
                controller.expirationDateElement.controller.rawFieldValue.testIn(this)
            val cvcControllerValueTurbine = controller.cvcElement.controller.rawFieldValue.testIn(this)

            assertThat(cardPillTurbine.awaitItem()).isNull()
            assertThat(numberControllerValueTurbine.awaitItem()).isEmpty()
            assertThat(expirationControllerValueTurbine.awaitItem()).isEmpty()
            assertThat(cvcControllerValueTurbine.awaitItem()).isEmpty()

            controller.onScannedCard(
                ScannedCardDetails.Validated(
                    cardNumber = "4242424242424242",
                    expirationYear = 2030,
                    expirationMonth = 6,
                )
            )

            controller.cvcElement.controller.onRawValueChange("323")

            idleLooper()

            assertThat(cardPillTurbine.awaitItem()).isNotNull()
            assertThat(numberControllerValueTurbine.awaitItem()).isEqualTo("4242424242424242")
            assertThat(expirationControllerValueTurbine.awaitItem()).isEqualTo("0630")
            assertThat(cvcControllerValueTurbine.awaitItem()).isEqualTo("323")

            composeTestRule.onNodeWithContentDescription(
                label = context.getString(R.string.stripe_scanned_card_pill_clear_content_description),
            )
                .performClick()

            composeTestRule.waitForIdle()

            assertThat(cardPillTurbine.awaitItem()).isNull()
            assertThat(numberControllerValueTurbine.awaitItem()).isEmpty()
            assertThat(expirationControllerValueTurbine.awaitItem()).isEmpty()
            assertThat(cvcControllerValueTurbine.awaitItem()).isEmpty()

            cardPillTurbine.cancelAndIgnoreRemainingEvents()
            numberControllerValueTurbine.cancelAndIgnoreRemainingEvents()
            expirationControllerValueTurbine.cancelAndIgnoreRemainingEvents()
            cvcControllerValueTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `When new card scanned with no expiry date, should clear date`() = runTest {
        val cardController = cardDetailsController(
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpYear to "2042",
                IdentifierSpec.CardExpMonth to "2",
                IdentifierSpec.CardCvc to "123",
            )
        )
        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("4242424242424242")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("242")
        assertThat(cardController.cvcElement.controller.rawFieldValue.value)
            .isEqualTo("123")

        idleLooper()

        cardController.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = null,
                expirationMonth = null,
            )
        )

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
    }

    @Test
    fun `When validated card scanned, CVC field gains focus`() = composeTest { controller ->
        composeTestRule.onNodeWithText(CVC_TEXT).assert(!isFocused())

        controller.onScannedCard(
            ScannedCardDetails.Validated(
                cardNumber = "4242424242424242",
                expirationYear = 2030,
                expirationMonth = 6,
            )
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(CVC_TEXT).assert(isFocused())
    }

    @Test
    fun `When card scanned via camera, CVC field does not gain focus`() = composeTest { controller ->
        composeTestRule.onNodeWithText(CVC_TEXT).assert(!isFocused())

        controller.onScannedCard(
            ScannedCardDetails.Unvalidated(
                cardNumber = "5555555555554444",
                expirationYear = 2044,
                expirationMonth = 4,
            )
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(CVC_TEXT).assert(!isFocused())
    }

    private fun composeTest(
        block: suspend (controller: CardDetailsController) -> Unit,
    ) = runTest {
        val cardController = cardDetailsController()

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalCardNumberCompletedEventReporter provides FakeCardNumberCompletedEventReporter
            ) {
                Column {
                    cardController.ComposeUI(
                        enabled = true,
                        field = CardDetailsElement(
                            identifier = IdentifierSpec.Generic("card_details"),
                            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
                            initialValues = mapOf(),
                            coroutineScope = coroutineScope,
                        ),
                        modifier = Modifier,
                        hiddenIdentifiers = emptySet(),
                        lastTextFieldIdentifier = null,
                    )
                }
            }
        }

        block(cardController)
    }

    private fun cardDetailsController(
        initialValues: Map<IdentifierSpec, String?> = emptyMap(),
        cbcEligibility: CardBrandChoiceEligibility = CardBrandChoiceEligibility.Ineligible,
        cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
        cardDetailsTextFieldConfig: CardNumberTextFieldConfig = CardNumberConfig(
            isCardBrandChoiceEligible = cbcEligibility != CardBrandChoiceEligibility.Ineligible,
            cardBrandFilter = cardBrandFilter
        ),
        cvcTextFieldConfig: CvcTextFieldConfig = CvcConfig(),
        dateConfig: TextFieldConfig = DateConfig(),
    ): CardDetailsController {
        return CardDetailsController(
            cardBrandFilter = cardBrandFilter,
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = initialValues,
            coroutineScope = coroutineScope,
            cbcEligibility = cbcEligibility,
            cardDetailsTextFieldConfig = cardDetailsTextFieldConfig,
            cvcTextFieldConfig = cvcTextFieldConfig,
            dateConfig = dateConfig,
            validationMessageComparator = object : FieldValidationMessageComparator {
                override fun compare(
                    a: FieldValidationMessage?,
                    b: FieldValidationMessage?
                ): Int {
                    return when {
                        a == null && b == null -> 0
                        a == null -> 1
                        b == null -> -1
                        else -> a.message.compareTo(b.message)
                    }
                }
            },
        )
    }

    private class FakeCardNumberTextFieldConfig(
        private val defaultCardNumberTextFieldConfig: CardNumberTextFieldConfig,
        var textFieldState: TextFieldState
    ) : CardNumberTextFieldConfig by defaultCardNumberTextFieldConfig {
        override fun determineState(
            brand: CardBrand,
            accountRanges: List<AccountRange>,
            number: String,
            numberAllowedDigits: Int
        ): TextFieldState {
            return textFieldState
        }
    }

    private class FakeCvcTextFieldConfig(
        private val defaultCvcTextFieldConfig: CvcTextFieldConfig,
        var textFieldState: TextFieldState
    ) : CvcTextFieldConfig by defaultCvcTextFieldConfig {
        override fun determineState(
            brand: CardBrand,
            accountRanges: List<AccountRange>,
            number: String,
            numberAllowedDigits: Int
        ): TextFieldState {
            return textFieldState
        }
    }

    private class FakeTextFieldConfig(
        private val defaultTextFieldConfig: TextFieldConfig,
        var textFieldState: TextFieldState
    ) : TextFieldConfig by defaultTextFieldConfig {
        override fun determineState(input: String): TextFieldState {
            return textFieldState
        }
    }

    private object FakeCardNumberCompletedEventReporter : CardNumberCompletedEventReporter {
        override fun onCardNumberCompleted() {
            // No-op
        }
    }

    private companion object {
        const val CVC_TEXT = "CVC"
    }
}
