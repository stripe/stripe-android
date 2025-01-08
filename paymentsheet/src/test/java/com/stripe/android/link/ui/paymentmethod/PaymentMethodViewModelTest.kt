package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() = runTest {
        val vm = createViewModel()

        assertThat(vm.state.value).isEqualTo(
            PaymentMethodState(
                isProcessing = false,
                formArguments = TestFactory.CARD_FORM_ARGS,
                formElements = TestFactory.CARD_FORM_ELEMENTS,
                primaryButtonState = PrimaryButtonState.Disabled,
                primaryButtonLabel = completePaymentButtonLabel(TestFactory.LINK_CONFIGURATION.stripeIntent)
            )
        )
    }

    @Test
    fun `test selection state gets updated when form values change`() = runTest {
        val formHelper: FormHelper = mock()
        val formValues: FormFieldValues = mock()

        val vm = createViewModel(
            formHelper = formHelper,
        )

        vm.formValuesChanged(formValues)

        assertThat(vm.state.value.paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        verify(formHelper).onFormFieldValuesChanged(eq(formValues), eq(PaymentMethod.Type.Card.code))
    }

    private fun createViewModel(
        formHelper: FormHelper = mock(),
    ): PaymentMethodViewModel {
        whenever(formHelper.formElementsForCode(PaymentMethod.Type.Card.code))
            .thenReturn(TestFactory.CARD_FORM_ELEMENTS)
        whenever(formHelper.createFormArguments(PaymentMethod.Type.Card.code))
            .thenReturn(TestFactory.CARD_FORM_ARGS)
        return PaymentMethodViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            formHelperFactory = { selectionUpdater ->
                whenever(formHelper.onFormFieldValuesChanged(any(), any())).thenAnswer {
                    selectionUpdater(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
                }
                formHelper
            }
        )
    }
}
