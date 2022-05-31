package com.stripe.android.link.ui.paymentmethod

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.injection.Injectable
import com.stripe.android.link.injection.FormViewModelSubcomponent
import com.stripe.android.link.injection.NonFallbackInjector
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
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
class FormViewModelTest {
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
        val formViewModel = FormViewModel(
            LayoutSpec.create(
                NameSpec(),
                EmailSpec(),
                CountrySpec(onlyShowCountryCodes = setOf("AT", "BE", "DE", "ES", "IT", "NL")),
                SaveForFutureUseSpec()
            ),
            resourceRepository = resourceRepository,
            transformSpecToElement = transformSpecToElements
        )

        val nameElement =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.address_label_name)
        val emailElement =
            getSectionFieldTextControllerWithLabel(formViewModel, R.string.email)

        nameElement?.onValueChange("joe")
        assertThat(
            formViewModel.completeFormValues.first()?.get(IdentifierSpec.Name)
        ).isNull()

        emailElement?.onValueChange("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.get(IdentifierSpec.Email)
                ?.value
        ).isEqualTo("joe@gmail.com")
        assertThat(
            formViewModel.completeFormValues.first()?.get(NameSpec().apiPath)
                ?.value
        ).isEqualTo("joe")

        emailElement?.onValueChange("invalid.email@IncompleteDomain")

        assertThat(
            formViewModel.completeFormValues.first()?.get(NameSpec().apiPath)
        ).isNull()
    }

    @Test
    fun `Factory gets initialized by Injector when Injector is available`() {
        val mockBuilder = mock<FormViewModelSubcomponent.Builder>()
        val mockSubcomponent = mock<FormViewModelSubcomponent>()
        val mockViewModel = mock<FormViewModel>()

        whenever(mockBuilder.build()).thenReturn(mockSubcomponent)
        whenever(mockBuilder.formSpec(any())).thenReturn(mockBuilder)
        whenever(mockSubcomponent.formViewModel).thenReturn(mockViewModel)

        val injector = object : NonFallbackInjector {
            override fun inject(injectable: Injectable<*>) {
                val factory = injectable as FormViewModel.Factory
                factory.subComponentBuilderProvider = Provider { mockBuilder }
            }
        }

        val factory = FormViewModel.Factory(
            mock(),
            injector
        )
        val factorySpy = spy(factory)
        val createdViewModel = factorySpy.create(FormViewModel::class.java)
        assertThat(createdViewModel).isEqualTo(mockViewModel)
    }

    private suspend fun getSectionFieldTextControllerWithLabel(
        formViewModel: FormViewModel,
        @StringRes label: Int
    ) =
        formViewModel.elements.first()!!
            .filterIsInstance<SectionElement>()
            .flatMap { it.fields }
            .filterIsInstance<SectionSingleFieldElement>()
            .map { it.controller }
            .filterIsInstance<TextFieldController>()
            .firstOrNull {
                it.label.first() == label
            }
}
