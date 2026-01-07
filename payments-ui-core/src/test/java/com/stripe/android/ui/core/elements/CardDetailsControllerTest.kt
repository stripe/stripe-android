package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.CardBrandFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.cardscan.CardScanResult
import com.stripe.android.ui.core.cardscan.ScannedCard
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.DateConfig
import com.stripe.android.uicore.elements.FieldValidationMessage
import com.stripe.android.uicore.elements.FieldValidationMessageComparator
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldConfig
import com.stripe.android.uicore.elements.TextFieldState
import com.stripe.android.uicore.elements.TextFieldStateConstants
import com.stripe.android.utils.TestUtils.idleLooper
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsControllerTest {

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
    fun `When eligible for card brand choice and preferred card brand is passed, initial value should have been set`() = runTest {
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

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2044,
            expirationMonth = 4,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("444")
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

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2044,
            expirationMonth = 4,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("444")
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

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = 2009,
            expirationMonth = 12,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
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

        val scannedCard = ScannedCard(
            pan = "5555555555554444",
            expirationYear = null,
            expirationMonth = null,
        )

        val cardScanResult = CardScanResult.Completed(
            scannedCard = scannedCard
        )
        idleLooper()

        cardController.onCardScanResult.invoke(cardScanResult)

        assertThat(cardController.numberElement.controller.rawFieldValue.value)
            .isEqualTo("5555555555554444")
        assertThat(cardController.expirationDateElement.controller.rawFieldValue.value)
            .isEqualTo("")
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
        override fun determineState(brand: CardBrand, number: String, numberAllowedDigits: Int): TextFieldState {
            return textFieldState
        }
    }

    private class FakeCvcTextFieldConfig(
        private val defaultCvcTextFieldConfig: CvcTextFieldConfig,
        var textFieldState: TextFieldState
    ) : CvcTextFieldConfig by defaultCvcTextFieldConfig {
        override fun determineState(brand: CardBrand, number: String, numberAllowedDigits: Int): TextFieldState {
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
}
