package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.lpmfoundations.luxe.TransformSpecToElements
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.HAS_OTHER_PAYMENT_METHODS_DEFAULT_VALUE
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SectionSingleFieldElement
import com.stripe.android.uicore.elements.TextFieldController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

@RunWith(RobolectricTestRunner::class)
class FormControllerTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        StripeR.style.StripeDefaultTheme
    )

    private val transformSpecToElements = TransformSpecToElements(
        UiDefinitionFactory.Arguments(
            initialValues = emptyMap(),
            initialLinkUserInput = null,
            saveForFutureUseInitialValue = false,
            merchantName = "Merchant",
            cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context),
            shippingValues = null,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
            requiresMandate = false,
            linkConfigurationCoordinator = null,
            onLinkInlineSignupStateChanged = { throw AssertionError("Not implemented") },
            cardBrandFilter = DefaultCardBrandFilter,
            hasOtherPaymentMethods = HAS_OTHER_PAYMENT_METHODS_DEFAULT_VALUE,
        )
    )

    @Test
    fun `Verify params are set when element flows are complete`() = runTest {
        // Using Sofort as a complex enough example to test the form view model class.
        val formController = FormController(
            LayoutSpec.create(
                NameSpec(),
                EmailSpec(),
                CountrySpec(allowedCountryCodes = setOf("AT", "BE", "DE", "ES", "IT", "NL")),
            ),
            transformSpecToElement = transformSpecToElements,
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formController, CoreR.string.stripe_address_label_full_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formController, UiCoreR.string.stripe_email)

        nameElement?.onValueChange("joe")
        assertThat(
            formController.completeFormValues.first()?.get(IdentifierSpec.Name)
        ).isNull()

        emailElement?.onValueChange("joe@gmail.com")
        assertThat(
            formController.completeFormValues.first()?.get(IdentifierSpec.Email)
                ?.value
        ).isEqualTo("joe@gmail.com")
        assertThat(
            formController.completeFormValues.first()?.get(NameSpec().apiPath)
                ?.value
        ).isEqualTo("joe")

        emailElement?.onValueChange("invalid.email@IncompleteDomain")

        assertThat(
            formController.completeFormValues.first()?.get(NameSpec().apiPath)
        ).isNull()
    }

    private suspend fun getSectionFieldTextControllerWithLabel(
        formController: FormController,
        @StringRes label: Int
    ) =
        formController.elements.first()
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull {
                it.label.first() == label
            }
}
