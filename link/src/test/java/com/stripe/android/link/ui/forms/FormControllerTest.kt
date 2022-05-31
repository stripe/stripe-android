package com.stripe.android.link.ui.forms

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.address.AddressFieldElementRepository
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionElement
import com.stripe.android.ui.core.elements.SectionSingleFieldElement
import com.stripe.android.ui.core.elements.TextFieldController
import com.stripe.android.ui.core.forms.TransformSpecToElements
import com.stripe.android.ui.core.forms.resources.StaticResourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormControllerTest {
    private val context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext(),
        R.style.StripeDefaultTheme
    )

    private val resourceRepository =
        StaticResourceRepository(
            AddressFieldElementRepository(
                ApplicationProvider.getApplicationContext<Context>().resources
            )
        )

    private val transformSpecToElements = TransformSpecToElements(
        resourceRepository = resourceRepository,
        initialValues = emptyMap(),
        amount = null,
        saveForFutureUseInitialValue = false,
        merchantName = "Merchant",
        context = context
    )

    @ExperimentalCoroutinesApi
    @Test
    fun `Verify params are set when element flows are complete`() = runTest {
        // Using Sofort as a complex enough example to test the form view model class.
        val formController = FormController(
            LayoutSpec.create(
                NameSpec(),
                EmailSpec(),
                CountrySpec(onlyShowCountryCodes = setOf("AT", "BE", "DE", "ES", "IT", "NL")),
                SaveForFutureUseSpec()
            ),
            resourceRepository = resourceRepository,
            transformSpecToElement = transformSpecToElements,
            this
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formController, R.string.address_label_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formController, R.string.email)

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
        formController.elements.first()!!
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull {
                it.label.first() == label
            }
}
