package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.TestFactory
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.link.ui.paymentmenthod.UpdateSelection
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.FormHelper
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
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

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
    fun `test selection and button state gets updated when form values change`() = runTest {
        val formHelper = PaymentMethodFormHelper()
        val formValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val vm = createViewModel(
            formHelper = formHelper,
        )

        vm.formValuesChanged(formValues)

        assertThat(vm.state.value.paymentSelection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(formHelper.onFormFieldValuesChangedCalls).containsExactly(
            PaymentMethodFormHelper.OnFormFieldValuesChangedCall(
                formValues = formValues,
                selectedPaymentMethodCode = PaymentMethod.Type.Card.code
            )
        )

        formHelper.paymentSelection = null
        vm.formValuesChanged(null)

        assertThat(vm.state.value.paymentSelection).isNull()
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
        assertThat(formHelper.onFormFieldValuesChangedCalls.last().formValues).isNull()
    }

    @Test
    fun `onPayClicked confirms payment successfully`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = { result = it }
        )

        // Simulate form completion
        viewModel.formValuesChanged(FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest))

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.calls).containsExactly(
            FakeLinkConfirmationHandler.Call.WithPaymentSelection(
                paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                linkAccount = TestFactory.LINK_ACCOUNT
            )
        )

        assertThat(result).isEqualTo(LinkActivityResult.Completed)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Processing)
    }

    @Test
    fun `onPayClicked handles confirmation failure`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        linkConfirmationHandler.confirmResult = LinkConfirmationResult.Failed("Payment failed".resolvableString)

        val viewModel = createViewModel(linkConfirmationHandler = linkConfirmationHandler)

        // Simulate form completion
        viewModel.formValuesChanged(FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest))

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.calls).hasSize(1)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(viewModel.state.value.errorMessage).isEqualTo("Payment failed".resolvableString)
    }

    @Test
    fun `onPayClicked handles cancellation`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        linkConfirmationHandler.confirmResult = LinkConfirmationResult.Canceled

        val viewModel = createViewModel(linkConfirmationHandler = linkConfirmationHandler)

        // Simulate form completion
        viewModel.formValuesChanged(FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest))

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.calls).hasSize(1)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(viewModel.state.value.errorMessage).isNull()
    }

    @Test
    fun `onPayClicked does nothing when payment selection is null`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val viewModel = createViewModel(linkConfirmationHandler = linkConfirmationHandler)

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.calls).isEmpty()
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
    }

    private fun createViewModel(
        formHelper: PaymentMethodFormHelper = PaymentMethodFormHelper(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): PaymentMethodViewModel {
        return PaymentMethodViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = dismissWithResult,
            formHelperFactory = { updateSelection ->
                formHelper.updateSelection = updateSelection
                formHelper
            }
        )
    }
}

private class PaymentMethodFormHelper : FormHelper {
    var updateSelection: UpdateSelection? = null
    var paymentSelection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
    val onFormFieldValuesChangedCalls = arrayListOf<OnFormFieldValuesChangedCall>()

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        onFormFieldValuesChangedCalls.add(
            OnFormFieldValuesChangedCall(
                formValues = formValues,
                selectedPaymentMethodCode = selectedPaymentMethodCode
            )
        )
        updateSelection?.invoke(paymentSelection)
    }

    override fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: String
    ): FormHelper.PaymentMethodParams? {
        TODO("Not yet implemented")
    }

    override fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        TODO("Not yet implemented")
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

    data class OnFormFieldValuesChangedCall(
        val formValues: FormFieldValues?,
        val selectedPaymentMethodCode: String
    )
}
