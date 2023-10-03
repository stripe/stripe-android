package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.stripecardscan.R
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsElementTest {

    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeCardScanDefaultTheme)

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

    @Test
    fun `test form field values returned when eligible for card brand choice`() = runTest {
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
            isEligibleForCardBrandChoice = true
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
                    IdentifierSpec.PreferredCardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                    IdentifierSpec.CardExpMonth to FormFieldEntry("01", true),
                    IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
                )
            )
        }
    }
}
