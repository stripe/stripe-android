package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.R
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.TextFieldIcon
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsElementTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeCardScanDefaultTheme)

    @Before
    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test form field values returned and expiration date parsing`() = runTest {
        val cardController = CardDetailsController(context, emptyMap())
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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
            context,
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
            context = context,
            initialValues = emptyMap(),
            collectName = true,
        )
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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

    @Ignore("Figure out why this succeeds in isolation but fails as part of the test suite")
    @Test
    fun `test form field values returned when eligible for card brand choice`() = runTest(testDispatcher) {
        val cbcEligibility = CardBrandChoiceEligibility.Eligible(preferredNetworks = emptyList())
        val cardController = CardDetailsController(
            context = context,
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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
                    IdentifierSpec.PreferredCardBrand to FormFieldEntry(null, true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
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
            context = context,
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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
                label = resolvableString(CardBrand.CartesBancaires.displayName),
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
            context = context,
            initialValues = emptyMap(),
            collectName = true,
            cbcEligibility = cbcEligibility
        )

        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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
}
