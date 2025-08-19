package com.stripe.android.ui.core.elements

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
class CardDetailsElementTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Test
    fun `test form field values returned and expiration date parsing`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            uiContext = testDispatcher,
            workContext = testDispatcher,
        )
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            controller = cardController
        )

        assertThat(cardDetailsElement.controller.nameElement).isNull()
        cardDetailsElement.controller.numberElement.controller.onValueChange("4242424242424242")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test view only form field values returned and expiration date parsing`() = runTest {
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardBrand to CardBrand.Visa.code
            )
        )

        assertThat(cardDetailsElement.controller.nameElement).isNull()
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("1230")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("12", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test form field values returned when collecting name`() = runTest {
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            uiContext = testDispatcher,
            workContext = testDispatcher,
        )
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            controller = cardController,
        )

        assertThat(cardDetailsElement.controller.nameElement).isNotNull()
        cardDetailsElement.controller.nameElement?.controller?.onValueChange("Jane Doe")
        cardDetailsElement.controller.numberElement.controller.onValueChange("4242424242424242")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test form field values returned when eligible for card brand choice`() = runTest(testDispatcher) {
        val cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList())
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility,
            uiContext = testDispatcher,
            workContext = testDispatcher,
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            controller = cardController,
            cbcEligibility = cbcEligibility
        )

        assertThat(cardDetailsElement.controller.nameElement).isNotNull()
        cardDetailsElement.controller.nameElement?.controller?.onValueChange("Jane Doe")
        cardDetailsElement.controller.numberElement.controller.onValueChange("4242424242424242")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.PreferredCardBrand to FormFieldEntry(null, true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test form field values returned when eligible for card brand choice and brand is changed`() = runTest {
        val cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf())
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility,
            uiContext = testDispatcher,
            workContext = testDispatcher,
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            controller = cardController,
            cbcEligibility = cbcEligibility
        )

        assertThat(cardDetailsElement.controller.nameElement).isNotNull()
        cardDetailsElement.controller.nameElement?.controller?.onValueChange("Jane Doe")
        cardDetailsElement.controller.numberElement.controller.onValueChange("4000002500001001")
        cardDetailsElement.controller.numberElement.controller.onDropdownItemClicked(
            TextFieldIcon.Dropdown.Item(
                id = CardBrand.CartesBancaires.code,
                label = CardBrand.CartesBancaires.displayName.resolvableString,
                icon = CardBrand.CartesBancaires.icon
            )
        )
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4000002500001001", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.PreferredCardBrand to FormFieldEntry("cartes_bancaires", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("cartes_bancaires", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test form field values returned when eligible for cbc & preferred network is passed`() = runTest {
        val cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf(CardBrand.CartesBancaires))
        val cardController = CardDetailsController(
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility,
            uiContext = testDispatcher,
            workContext = testDispatcher,
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            controller = cardController,
            cbcEligibility = cbcEligibility
        )

        assertThat(cardDetailsElement.controller.nameElement).isNotNull()
        cardDetailsElement.controller.nameElement?.controller?.onValueChange("Jane Doe")
        cardDetailsElement.controller.numberElement.controller.onValueChange("4000002500001001")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        cardDetailsElement.getFormFieldValueFlow().test {
            assertThat(awaitItem()).containsExactlyElementsIn(
                listOf(
                    IdentifierSpec.Name to FormFieldEntry("Jane Doe", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4000002500001001", true),
                    IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                    IdentifierSpec.PreferredCardBrand to FormFieldEntry("cartes_bancaires", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("cartes_bancaires", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }

    @Test
    fun `test when validating, all fields show errors as expected`() = runTest {
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            initialValues = emptyMap(),
            collectName = true,
            controller = CardDetailsController(
                cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
                initialValues = emptyMap(),
                collectName = true,
                cbcEligibility = CardBrandChoiceEligibility.Ineligible,
                uiContext = testDispatcher,
                workContext = testDispatcher,
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(cardDetailsElement.controller.nameElement).isNotNull()

        val nameElement = requireNotNull(cardDetailsElement.controller.nameElement)

        nameElement.errorTest(null)
        cardDetailsElement.controller.numberElement.errorTest(null)
        cardDetailsElement.controller.cvcElement.errorTest(null)
        cardDetailsElement.controller.expirationDateElement.errorTest(null)

        cardDetailsElement.onValidationStateChanged(isValidating = true)

        nameElement
            .errorTest(FieldError(UiCoreR.string.stripe_blank_and_required))
        cardDetailsElement.controller.numberElement
            .errorTest(FieldError(UiCoreR.string.stripe_blank_and_required))
        cardDetailsElement.controller.cvcElement
            .errorTest(FieldError(UiCoreR.string.stripe_blank_and_required))
        cardDetailsElement.controller.expirationDateElement
            .errorTest(FieldError(UiCoreR.string.stripe_blank_and_required))

        nameElement.controller.onValueChange("Sa")
        cardDetailsElement.controller.numberElement.controller.onValueChange("4000")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("32")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("29")

        nameElement.errorTest(null)
        cardDetailsElement.controller.numberElement
            .errorTest(FieldError(StripeR.string.stripe_invalid_card_number))
        cardDetailsElement.controller.cvcElement
            .errorTest(FieldError(StripeR.string.stripe_invalid_cvc))
        cardDetailsElement.controller.expirationDateElement
            .errorTest(FieldError(UiCoreR.string.stripe_incomplete_expiry_date))
    }
}
