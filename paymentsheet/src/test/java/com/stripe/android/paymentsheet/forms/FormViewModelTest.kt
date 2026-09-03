package com.stripe.android.paymentsheet.forms

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures.COMPOSE_FRAGMENT_ARGS
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.utils.ViewModelStoreTestRule
import com.stripe.android.ui.core.elements.AffirmHeaderElement
import com.stripe.android.ui.core.elements.StaticTextElement
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FormViewModelTest {
    @get:Rule
    val viewModelStoreRule = ViewModelStoreTestRule()

    @Test
    fun `Verify completeFormValues is not null when no elements exist`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val formViewModel = createViewModel(
            arguments = args,
            formElements = emptyList(),
        )

        assertThat(
            formViewModel.completeFormValues.first()
        ).isNotNull()
    }

    @Test
    fun `Test default values are filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    line2 = "456",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEqualTo(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry("mail@mail.com", isComplete = true),
                    IdentifierSpec.Phone to FormFieldEntry("+13105551234", isComplete = true),
                    IdentifierSpec.Line1 to FormFieldEntry("123 Main Street", isComplete = true),
                    IdentifierSpec.Line2 to FormFieldEntry("456", isComplete = true),
                    IdentifierSpec.City to FormFieldEntry("San Francisco", isComplete = true),
                    IdentifierSpec.State to FormFieldEntry("CA", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry("94111", isComplete = true),
                )
            )
        }
    }

    @Test
    fun `Test only provided default values are filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                address = PaymentSheet.Address(
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = true,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEqualTo(
                mapOf(
                    IdentifierSpec.Name to FormFieldEntry("Jenny Rosen", isComplete = true),
                    IdentifierSpec.Email to FormFieldEntry("mail@mail.com", isComplete = true),
                    IdentifierSpec.Country to FormFieldEntry("US", isComplete = true),
                    IdentifierSpec.PostalCode to FormFieldEntry("94111", isComplete = true),
                )
            )
        }
    }

    @Test
    fun `Test default values are not filled`() = runTest {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code,
            billingDetails = PaymentSheet.BillingDetails(
                name = "Jenny Rosen",
                email = "mail@mail.com",
                phone = "+13105551234",
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    line2 = "456",
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111"
                ),
            ),
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                attachDefaultsToPaymentMethod = false,
            )
        )

        val viewModel = createViewModel(
            args,
            emptyList(),
        )

        viewModel.completeFormValues.test {
            assertThat(awaitItem()?.fieldValuePairs).isEmpty()
        }
    }

    @Test
    fun `updateFormElements updates elements when identifiers match but types differ`() {
        val args = COMPOSE_FRAGMENT_ARGS.copy(
            paymentMethodCode = PaymentMethod.Type.Card.code
        )
        val originalElements = listOf(
            AffirmHeaderElement(
                identifier = IdentifierSpec.Generic("affirm_promotion")
            ),
        )
        val formViewModel = createViewModel(args, originalElements)

        assertThat(formViewModel.elements).isEqualTo(originalElements)

        val newElements = listOf(
            StaticTextElement(
                identifier = IdentifierSpec.Generic("affirm_promotion"),
                text = resolvableString("Static text"),
            ),
        )
        formViewModel.updateFormElements(newElements)

        assertThat(formViewModel.elements).isEqualTo(newElements)
    }

    private fun createViewModel(
        arguments: FormArguments,
        formElements: List<FormElement>,
    ) = FormViewModel(
        formArguments = arguments,
        formElements = formElements,
    ).also { viewModelStoreRule.track(it) }
}
