package com.stripe.android.link.ui.paymentmethod

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.FakeLinkAccountManager
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.confirmation.FakeLinkConfirmationHandler
import com.stripe.android.link.confirmation.LinkConfirmationHandler
import com.stripe.android.link.ui.PrimaryButtonState
import com.stripe.android.link.ui.completePaymentButtonLabel
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodState
import com.stripe.android.link.ui.paymentmenthod.PaymentMethodViewModel
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.FakeLogger
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
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

        assertThat(vm.state.value.paymentMethodCreateParams).isEqualTo(PaymentMethodCreateParamsFixtures.DEFAULT_CARD)
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(formHelper.getPaymentMethodParamsCalls).containsExactly(
            PaymentMethodFormHelper.GetPaymentMethodParamsCall(
                formValues = formValues,
                selectedPaymentMethodCode = PaymentMethod.Type.Card.code
            )
        )

        formHelper.paymentMethodCreateParams = null
        vm.formValuesChanged(null)

        assertThat(vm.state.value.paymentMethodCreateParams).isNull()
        assertThat(vm.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
        assertThat(formHelper.getPaymentMethodParamsCalls.last().formValues).isNull()
    }

    @Test
    fun `onPayClicked confirms payment successfully`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val linkAccountManager = FakeLinkAccountManager()
        var result: LinkActivityResult? = null
        val viewModel = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            dismissWithResult = { result = it }
        )

        viewModel.formValuesChanged(
            formValues = FormFieldValues(
                fieldValuePairs = mapOf(IdentifierSpec.CardCvc to FormFieldEntry("111")),
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest
            )
        )

        viewModel.onPayClicked()

        val call = linkConfirmationHandler.confirmWithLinkPaymentDetailsCall.first()
        assertThat(call.paymentDetails)
            .isEqualTo(TestFactory.LINK_NEW_PAYMENT_DETAILS)
        assertThat(call.cvc).isEqualTo("111")
        assertThat(result).isEqualTo(LinkActivityResult.Completed)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
    }

    @Test
    fun `onPayClicked handles payment method creation failure correctly`() = runTest {
        val error = Throwable("oops")
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val linkAccountManager = FakeLinkAccountManager()
        val logger = FakeLogger()
        var result: LinkActivityResult? = null

        linkAccountManager.createCardPaymentDetailsResult = Result.failure(error)

        val viewModel = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler,
            linkAccountManager = linkAccountManager,
            logger = logger,
            dismissWithResult = { result = it }
        )

        viewModel.formValuesChanged(
            formValues = FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest)
        )

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).isEmpty()

        assertThat(result).isEqualTo(null)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(viewModel.state.value.errorMessage).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(logger.errorLogs).containsExactly(
            "PaymentMethodViewModel: Failed to create card payment details" to error
        )
    }

    @Test
    fun `onPayClicked handles confirmation failure`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()

        linkConfirmationHandler.confirmWithLinkPaymentDetailsResult =
            LinkConfirmationResult.Failed("Payment failed".resolvableString)

        val viewModel = createViewModel(linkConfirmationHandler = linkConfirmationHandler)

        viewModel.formValuesChanged(
            formValues = FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest)
        )

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).hasSize(1)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(viewModel.state.value.errorMessage).isEqualTo("Payment failed".resolvableString)
    }

    @Test
    fun `onPayClicked handles cancellation`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        linkConfirmationHandler.confirmWithLinkPaymentDetailsResult = LinkConfirmationResult.Canceled

        val viewModel = createViewModel(linkConfirmationHandler = linkConfirmationHandler)

        viewModel.formValuesChanged(
            formValues = FormFieldValues(userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest)
        )

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).hasSize(1)
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Enabled)
        assertThat(viewModel.state.value.errorMessage).isNull()
    }

    @Test
    fun `onPayClicked logs when payment selection is null`() = runTest {
        val linkConfirmationHandler = FakeLinkConfirmationHandler()
        val logger = FakeLogger()

        val viewModel = createViewModel(
            linkConfirmationHandler = linkConfirmationHandler,
            logger = logger
        )

        viewModel.onPayClicked()

        assertThat(linkConfirmationHandler.confirmWithLinkPaymentDetailsCall).isEmpty()
        assertThat(viewModel.state.value.primaryButtonState).isEqualTo(PrimaryButtonState.Disabled)
        assertThat(logger.errorLogs)
            .containsExactly("PaymentMethodViewModel: onPayClicked without paymentMethodCreateParams" to null)
    }

    private fun createViewModel(
        formHelper: PaymentMethodFormHelper = PaymentMethodFormHelper(),
        linkConfirmationHandler: LinkConfirmationHandler = FakeLinkConfirmationHandler(),
        linkAccountManager: LinkAccountManager = FakeLinkAccountManager(),
        logger: Logger = FakeLogger(),
        dismissWithResult: (LinkActivityResult) -> Unit = {}
    ): PaymentMethodViewModel {
        return PaymentMethodViewModel(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT,
            linkConfirmationHandler = linkConfirmationHandler,
            dismissWithResult = dismissWithResult,
            formHelper = formHelper,
            logger = logger,
            linkAccountManager = linkAccountManager
        )
    }
}
