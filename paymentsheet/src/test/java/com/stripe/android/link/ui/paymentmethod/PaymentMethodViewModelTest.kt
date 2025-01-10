package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.link.ui.paymentmenthod.UpdateSelection
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FakeFormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
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
        var paramFormValues: FormFieldValues? = null
        val formHelper = object : PaymentMethodFormHelper() {
            override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
                paramFormValues = formValues
                super.onFormFieldValuesChanged(formValues, selectedPaymentMethodCode)
            }
        }
        val formValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val vm = createViewModel(
            formHelper = formHelper,
        )

        vm.formValuesChanged(formValues)

        assertThat(vm.state.value.paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(paramFormValues).isEqualTo(formValues)
    }

    private fun createViewModel(
        formHelper: PaymentMethodFormHelper = PaymentMethodFormHelper(),
    ): PaymentMethodViewModel {
        return PaymentMethodViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            formHelperFactory = { updateSelection ->
                formHelper.updateSelection = updateSelection
                formHelper
            }
        )
    }
}

private open class PaymentMethodFormHelper : FakeFormHelper() {
    var updateSelection: UpdateSelection? = null

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        updateSelection?.invoke(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    override fun formElementsForCode(code: String): List<FormElement> {
        require(code == PaymentMethod.Type.Card.code) {
            "$code payment not supported"
        }
        return TestFactory.CARD_FORM_ELEMENTS
    }

    override fun createFormArguments(paymentMethodCode: PaymentMethodCode): FormArguments {
        require(paymentMethodCode == PaymentMethod.Type.Card.code) {
            "$paymentMethodCode payment not supported"
        }
        return TestFactory.CARD_FORM_ARGS
    }
}
