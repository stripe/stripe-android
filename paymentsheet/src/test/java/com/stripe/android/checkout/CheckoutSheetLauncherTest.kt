package com.stripe.android.checkout

import android.app.Application
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelperStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.DummyActivityResultCaller.RegisterCall
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.FakeStripeImageLoader
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.asCallbackFor
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
internal class CheckoutSheetLauncherTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigurationTestRule = PaymentConfigurationTestRule(applicationContext)

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `launchForm launches activity with correct parameters`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = createCustomerState()
        val promotion = PaymentMethodMessagePromotion(
            paymentMethodType = "KLARNA",
            message = "Message",
            learnMore = PaymentMethodMessageLearnMore(
                message = "Message",
                url = "https://www.test.com",
            ),
        )
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            productUsage = setOf("Checkout"),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = null,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            promotions = listOf(promotion),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = code,
            ),
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = customerState,
            promotion = promotion,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()
        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo(code)
    }

    @Test
    fun `launchForm launches activity with current selection when selection matches code`() = testScenario {
        val code = "card"
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `launchForm launches activity with previous form details`() = testScenario {
        val code = "card"
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `launchForm launches activity with null selection when selection is a saved card`() = testScenario {
        val code = "card"
        selectionHolder.setSelection(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isNull()
    }

    @Test
    fun `launchForm launches activity with null selection when selection is for another LPM`() = testScenario {
        val code = "card"
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isNull()
    }

    @Test
    fun `launchForm logs error and returns if configuration is null`() = testScenario {
        sheetLauncher.launchForm(
            code = "test_code",
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = null,
            customerState = createCustomerState(),
            promotion = null,
        )
        val loggedErrors = errorReporter.getLoggedErrors()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first())
            .isEqualTo("unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null")
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
    }

    @Test
    fun `launchForm is not launched again when the sheet is already open`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchForm(
            code = "test_code",
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
    }

    @Test
    fun `formActivityLauncher sets selection and customer state on complete result`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        launchForm("cashapp")

        val customerState = createCustomerState()
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
    }

    @Test
    fun `formActivityLauncher invokes immediate action when complete result has selection`() = testScenario {
        launchForm("cashapp")
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "cashapp"),
        )

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)

        assertThat(immediateActionWasInvoked()).isTrue()
    }

    @Test
    fun `formActivityLauncher does not invoke immediate action when the result is confirmed`() = testScenario {
        launchForm("cashapp")
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = true,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "cashapp"),
        )

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)

        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `formActivityLauncher refreshes checkout session from complete result`() = testScenario {
        val response = CheckoutSessionResponseFactory.create()
        sessionRefresher.enqueueRefreshAction {}
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = response,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        runCurrent()

        assertThat(awaitRefreshCall()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
    }

    @Test
    fun `formActivityLauncher does not refresh checkout session when complete result has no response`() = testScenario {
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        runCurrent()

        expectNoRefreshCalls()
    }

    @Test
    fun `formActivityLauncher sets customer state but keeps selection on cancelled result`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        launchForm("card")

        val customerState = createCustomerState()
        val result = EmbeddedActivityResult.Cancelled(
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
    }

    @Test
    fun `formActivityLauncher does not update state on error result`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        launchForm("card")

        val result = EmbeddedActivityResult.Error(
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `form result handled correctly without prior launchForm call (simulates host recreation)`() = testScenario {
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            hasBeenConfirmed = true,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            productUsage = setOf("Checkout"),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = PaymentSelection.GooglePay,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.Manage,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )

        sheetLauncher.launchManage(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = PaymentSelection.GooglePay,
            configuration = EmbeddedConfigurationFactory.create(),
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()

        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchManage logs error and returns if configuration is null`() = testScenario {
        sheetLauncher.launchManage(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            selection = PaymentSelection.GooglePay,
            configuration = null,
        )
        val loggedErrors = errorReporter.getLoggedErrors()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first())
            .isEqualTo("unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null")
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `launchManage is not launched again when the sheet is already open`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchManage(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            selection = PaymentSelection.GooglePay,
            configuration = EmbeddedConfigurationFactory.create(),
        )
    }

    @Test
    fun `manageSheetLauncher callback updates state on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            selection = selection,
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Manage,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `manageSheetLauncher invokes immediate action for saved selection when flagged`() = testScenario {
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = true,
            launchMode = EmbeddedLaunchMode.Manage,
        )

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)

        assertThat(immediateActionWasInvoked()).isTrue()
    }

    @Test
    fun `manageSheetLauncher refreshes checkout session after save without invoking immediate action`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val response = CheckoutSessionResponseFactory.create(id = "saved_card_tax_response")
        val result = manageCompleteResult(checkoutSessionResponse = response).copy(
            shouldInvokeSelectionCallback = false,
        )

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)
        runCurrent()

        assertThat(awaitRefreshCall()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(selectionHolder.selection.value).isEqualTo(result.selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `manageSheetLauncher invokes immediate action before checkout session refresh completes`() = testScenario {
        val releaseRefresh = CompletableDeferred<Unit>()
        val response = CheckoutSessionResponseFactory.create()
        sessionRefresher.enqueueRefreshAction { releaseRefresh.await() }
        val result = manageCompleteResult(checkoutSessionResponse = response)

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)
        runCurrent()

        assertThat(selectionHolder.selection.value).isEqualTo(result.selection)
        assertThat(awaitRefreshCall()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(immediateActionWasInvoked()).isTrue()

        releaseRefresh.complete(Unit)
        runCurrent()

        assertThat(immediateActionWasInvoked()).isTrue()
    }

    @Test
    fun `manageSheetLauncher invokes immediate action when checkout session refresh fails`() = testScenario {
        val response = CheckoutSessionResponseFactory.create()
        val expectedError = IllegalStateException("Refresh failed")
        sessionRefresher.enqueueRefreshAction { throw expectedError }
        val result = manageCompleteResult(checkoutSessionResponse = response)

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)
        runCurrent()

        assertThat(selectionHolder.selection.value).isEqualTo(result.selection)
        assertThat(awaitRefreshCall()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(immediateActionWasInvoked()).isTrue()
        assertThat(logger.errorLogs).containsExactly(
            "Failed to refresh the checkout session after the sheet closed." to expectedError
        )
    }

    private fun manageCompleteResult(
        checkoutSessionResponse: CheckoutSessionResponse,
    ) = EmbeddedActivityResult.Complete(
        previousNewSelections = Bundle(),
        customerState = null,
        linkAccountInfo = LinkAccountUpdate.Value(null),
        selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
        hasBeenConfirmed = false,
        checkoutSessionResponse = checkoutSessionResponse,
        shouldInvokeSelectionCallback = true,
        launchMode = EmbeddedLaunchMode.Manage,
    )

    @Test
    fun `manageSheetLauncher callback does not update state on cancelled result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)),
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.Manage,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `manageSheetLauncher callback does not update state on error result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.Manage)
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `launchPaymentOptions launches activity with correct parameters`() = testScenario(
        promotions = listOf(FakePaymentMethodMessagePromotionsHelper.klarnaPromotion),
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.GooglePay
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            productUsage = setOf("Checkout"),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = selection,
            previousNewSelections = selectionHolder.previousNewSelections,
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            promotions = listOf(FakePaymentMethodMessagePromotionsHelper.klarnaPromotion),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            presentationState = EmbeddedActivityArgs.PresentationState.Ready,
        )

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = selection,
            configuration = EmbeddedConfigurationFactory.create(),
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall()

        assertThat(launchCall).isEqualTo(expectedArgs)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `updating launch sends loading then refreshed ready arguments`() = testScenario {
        val mutationGate = CompletableDeferred<Unit>()
        coroutineScope.launch {
            operationCoordinator.runMutation {
                mutationGate.await()
                Result.success(Unit)
            }
        }
        runCurrent()

        val initialState = requireNotNull(embeddedContentState.value)
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = initialState.paymentMethodMetadata,
            customerState = null,
            selection = null,
            configuration = initialState.configuration,
        )
        val loadingArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(loadingArgs.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        assertThat(loadingArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Loading)

        val refreshedMetadata = PaymentMethodMetadataFactory.create()
        val refreshedConfiguration = EmbeddedConfigurationFactory.create(merchantDisplayName = "Refreshed merchant")
        val refreshedCustomer = createCustomerState()
        embeddedContentState.value = EmbeddedContentHelperStateHolder.State(
            paymentMethodMetadata = refreshedMetadata,
            embeddedViewDisplaysMandateText = true,
            configuration = refreshedConfiguration,
        )
        customerStateHolder.setCustomerState(refreshedCustomer)
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        mutationGate.complete(Unit)
        runCurrent()

        val readyArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(readyArgs.launchMode).isEqualTo(EmbeddedLaunchMode.PaymentOptions)
        assertThat(readyArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
        assertThat(readyArgs.paymentMethodMetadata).isEqualTo(refreshedMetadata)
        assertThat(readyArgs.configuration).isEqualTo(refreshedConfiguration)
        assertThat(readyArgs.customerState).isEqualTo(refreshedCustomer)
        assertThat(readyArgs.selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `recreated launcher sends ready arguments when mutation finishes`() = testScenario {
        createLinkPaymentOptionsPresenter()
        val mutationGate = CompletableDeferred<Unit>()
        coroutineScope.launch {
            operationCoordinator.runMutation {
                mutationGate.await()
                Result.success(Unit)
            }
        }
        runCurrent()

        val initialState = requireNotNull(embeddedContentState.value)
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = initialState.paymentMethodMetadata,
            customerState = null,
            selection = null,
            configuration = initialState.configuration,
        )
        val loadingArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(loadingArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Loading)
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isTrue()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()
        assertThat(SheetStateHolder(savedStateHandle).sheetIsOpen).isTrue()
        assertThat(CheckoutSheetLauncherState(savedStateHandle).isAwaitingPaymentOptionsReady).isTrue()
        mutationGate.complete(Unit)
        runCurrent()

        val recreatedLauncherState = CheckoutSheetLauncherState(savedStateHandle)
        recreateSheetLauncher(TestLifecycleOwner(), recreatedLauncherState)
        runCurrent()

        val readyArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(readyArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
        assertThat(recreatedLauncherState.isAwaitingPaymentOptionsReady).isFalse()
    }

    @Test
    fun `failed mutation sends retained state as ready arguments`() = testScenario {
        val mutationGate = CompletableDeferred<Unit>()
        coroutineScope.launch {
            operationCoordinator.runMutation<Unit> {
                mutationGate.await()
                Result.failure(IllegalStateException("Failed mutation"))
            }
        }
        runCurrent()

        val retainedState = requireNotNull(embeddedContentState.value)
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = retainedState.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            configuration = retainedState.configuration,
        )
        val loadingArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(loadingArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Loading)

        mutationGate.complete(Unit)
        runCurrent()

        val readyArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(readyArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Ready)
        assertThat(readyArgs.paymentMethodMetadata).isEqualTo(retainedState.paymentMethodMetadata)
        assertThat(readyArgs.configuration).isEqualTo(retainedState.configuration)
    }

    @Test
    fun `missing refreshed state does not crash when mutation finishes`() = testScenario {
        val mutationGate = CompletableDeferred<Unit>()
        coroutineScope.launch {
            operationCoordinator.runMutation {
                mutationGate.await()
                Result.success(Unit)
            }
        }
        runCurrent()

        val initialState = requireNotNull(embeddedContentState.value)
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = initialState.paymentMethodMetadata,
            customerState = null,
            selection = null,
            configuration = initialState.configuration,
        )
        val loadingArgs = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(loadingArgs.presentationState).isEqualTo(EmbeddedActivityArgs.PresentationState.Loading)

        embeddedContentState.value = null
        mutationGate.complete(Unit)
        runCurrent()

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            "unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null"
        )
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isTrue()
    }

    @Test
    fun `cancelling loading suppresses ready launch`() = testScenario {
        val mutationGate = CompletableDeferred<Unit>()
        coroutineScope.launch {
            operationCoordinator.runMutation {
                mutationGate.await()
                Result.success(Unit)
            }
        }
        runCurrent()

        val state = requireNotNull(embeddedContentState.value)
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = null,
            selection = null,
            configuration = state.configuration,
        )
        dummyActivityResultCallerScenario.awaitLaunchCall()

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(
            EmbeddedActivityResult.Cancelled(
                customerState = null,
                linkAccountInfo = LinkAccountUpdate.Value(null),
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )
        )
        mutationGate.complete(Unit)
        runCurrent()

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isFalse()
    }

    @Test
    fun `launchPaymentOptions forwards previously entered new selections into the sheet`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = createCustomerState(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            configuration = EmbeddedConfigurationFactory.create(),
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs

        assertThat(launchCall.previousNewSelections.previousNewSelection("card"))
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(launchCall.previousNewSelections.previousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `launchPaymentOptions logs error and returns if configuration is null`() = testScenario {
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = null,
            selection = null,
            configuration = null,
        )
        val loggedErrors = errorReporter.getLoggedErrors()
        assertThat(loggedErrors.size).isEqualTo(1)
        assertThat(loggedErrors.first())
            .isEqualTo("unexpected_error.embedded.embedded_sheet_launcher.embedded_state_is_null")
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `launchPaymentOptions is not launched again when the sheet is already open`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = null,
            selection = null,
            configuration = EmbeddedConfigurationFactory.create(),
        )
    }

    @Test
    fun `paymentOptionsResult merges returned previous new selections into selection holder`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val returnedSelections = Bundle().apply {
            stashNewSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        }
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = returnedSelections,
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            selection = null,
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `paymentOptionsResult callback updates state on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val linkAccountInfo = LinkAccountUpdate.Value(
            account = null,
            lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed,
        )
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = customerState,
            linkAccountInfo = linkAccountInfo,
            selection = selection,
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(linkAccountInfo)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult contains checkout session refresh failure`() = testScenario {
        val response = CheckoutSessionResponseFactory.create()
        val expectedError = IllegalStateException("Refresh failed")
        sessionRefresher.enqueueRefreshAction { throw expectedError }
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = null,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            checkoutSessionResponse = response,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        runCurrent()

        assertThat(awaitRefreshCall()).isEqualTo(FakeCheckoutSessionRefresher.Call.Commit(response))
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(logger.errorLogs).containsExactly(
            "Failed to refresh the checkout session after the sheet closed." to expectedError
        )
        assertThat(operationCoordinator.isUpdating.value).isFalse()
    }

    @Test
    fun `paymentOptionsResult callback updates customer state on cancelled result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val result = EmbeddedActivityResult.Cancelled(
            customerState = customerState,
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptions sheet bridges Link account state`() = testScenario {
        val initialLinkAccountInfo = LinkAccountUpdate.Value(
            account = null,
            lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
        )
        val updatedLinkAccountInfo = LinkAccountUpdate.Value(
            account = null,
            lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed,
        )
        linkAccountHolder.set(initialLinkAccountInfo)

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = null,
            selection = null,
            configuration = EmbeddedConfigurationFactory.create(),
        )

        val args = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(args.linkAccountInfo).isEqualTo(initialLinkAccountInfo)

        registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(
            EmbeddedActivityResult.Cancelled(
                customerState = null,
                linkAccountInfo = updatedLinkAccountInfo,
                launchMode = EmbeddedLaunchMode.PaymentOptions,
            )
        )
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(updatedLinkAccountInfo)
    }

    @Test
    fun `paymentOptionsResult cancelled projects edited saved card billing details`() = testScenario {
        val original = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD.copy(
            billingDetails = PaymentMethodFixtures.BILLING_DETAILS.toBuilder()
                .setAddress(PaymentMethodFixtures.BILLING_DETAILS.address!!.copy(postalCode = "94107"))
                .build(),
        )
        val options = PaymentMethodOptionsParams.Card(network = "cartes_bancaires")
        val input = UserInput.SignIn("customer@example.com")
        val selection = PaymentSelection.Saved(original, options, input)
        val updated = original.copy(
            billingDetails = original.billingDetails!!.toBuilder()
                .setAddress(original.billingDetails!!.address!!.copy(postalCode = "94110"))
                .build(),
            card = original.card!!.copy(expiryMonth = 12, expiryYear = 2035),
        )
        selectionHolder.setSelection(selection)
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(original)))

        selectionHolder.session.test {
            val initial = awaitItem()!!.paymentOption!!
            assertThat(initial.billingDetails?.address?.postalCode).isEqualTo("94107")
            cancelPaymentOptions(createCustomerState(paymentMethods = listOf(updated)))
            val option = awaitItem()!!.paymentOption!!
            assertThat(option.billingDetails?.address?.postalCode).isEqualTo("94110")
            assertThat(option.label).isEqualTo(initial.label)
            val saved = selectionHolder.selection.value as PaymentSelection.Saved
            assertThat(saved.paymentMethod.id).isEqualTo(original.id)
            assertThat(saved.paymentMethod.card?.displayBrand).isEqualTo(original.card?.displayBrand)
            assertThat(saved.paymentMethod.card?.expiryMonth).isEqualTo(12)
            assertThat(saved.paymentMethod.card?.expiryYear).isEqualTo(2035)
            assertThat(saved.paymentMethodOptionsParams).isSameInstanceAs(options)
            assertThat(saved.linkInput).isSameInstanceAs(input)
        }
        runCurrent()
        expectNoRefreshCalls()
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `paymentOptionsResult cancelled editing unselected card preserves selection`() = testScenario {
        val selected = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val other = selected.copy(id = "pm_other")
        val updatedOther = other.copy(card = other.card!!.copy(expiryYear = 2035))
        val selection = PaymentSelection.Saved(selected)
        selectionHolder.setSelection(selection)
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(selected, other)))

        selectionHolder.session.test {
            awaitItem()
            cancelPaymentOptions(
                createCustomerState(paymentMethods = listOf(selected, updatedOther))
            )
            expectNoEvents()
        }
        assertThat(selectionHolder.selection.value).isSameInstanceAs(selection)
        expectNoRefreshCalls()
    }

    @Test
    fun `paymentOptionsResult cancelled without customer state preserves saved selection`() = testScenario {
        val selected = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val selection = PaymentSelection.Saved(selected)
        selectionHolder.setSelection(selection)
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(selected)))

        cancelPaymentOptions(null)

        assertThat(selectionHolder.selection.value).isSameInstanceAs(selection)
        expectNoRefreshCalls()
    }

    @Test
    fun `paymentOptionsResult cancelled preserves new selection`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.setSelection(selection)

        cancelPaymentOptions(createCustomerState(paymentMethods = emptyList()))

        assertThat(selectionHolder.selection.value).isSameInstanceAs(selection)
        expectNoRefreshCalls()
    }

    @Test
    fun `paymentOptionsResult cancelled preserves null selection`() = testScenario {
        cancelPaymentOptions(createCustomerState())

        assertThat(selectionHolder.selection.value).isNull()
        expectNoRefreshCalls()
    }

    @Test
    fun `paymentOptionsResult cancelled clears stale saved selection`() = testScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        selectionHolder.setSelection(PaymentSelection.Saved(paymentMethod))
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))

        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = emptyList()),
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        selectionHolder.session.test {
            assertThat(awaitItem()?.paymentOption).isNotNull()
            registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(result)
            assertThat(awaitItem()?.paymentOption).isNull()
        }

        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        expectNoRefreshCalls()
    }

    @Test
    fun `paymentOptionsResult cancelled rebinds saved selection to updated payment method`() = testScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
        val updatedPaymentMethod = paymentMethod.copy(
            card = PaymentMethodFixtures.CARD_WITH_NETWORKS.copy(displayBrand = "visa")
        )
        selectionHolder.setSelection(PaymentSelection.Saved(paymentMethod))
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))

        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = listOf(updatedPaymentMethod)),
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(updatedPaymentMethod))
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult cancelled preserves valid saved selection`() = testScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val savedSelection = PaymentSelection.Saved(paymentMethod)
        selectionHolder.setSelection(savedSelection)
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))

        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = listOf(paymentMethod)),
            linkAccountInfo = LinkAccountUpdate.Value(null),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(selectionHolder.selection.value).isSameInstanceAs(savedSelection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult does not update state on error result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val linkAccountInfo = LinkAccountUpdate.Value(
            account = null,
            lastUpdateReason = LinkAccountUpdate.Value.UpdateReason.LoggedOut,
        )
        linkAccountHolder.set(linkAccountInfo)
        val result = EmbeddedActivityResult.Error(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(linkAccountHolder.linkAccountInfo.value).isEqualTo(linkAccountInfo)
        assertThat(selectionHolder.selection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `onDestroy unregisters launcher`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val unregisteredLauncher = dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        assertThat(unregisteredLauncher).isEqualTo(launcher)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchForm before creation leaves state unchanged`() = testScenario(
        initialLifecycleState = Lifecycle.State.INITIALIZED,
    ) {
        selectionHolder.setTemporarySelection("existing")
        val state = selectionHolder.state

        sheetLauncher.launchForm(
            code = "card",
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = null,
            promotion = null,
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("existing")
        assertThat(selectionHolder.state).isEqualTo(state)
        assertThat(immediateActionWasInvoked()).isFalse()
        assertThat(errorReporter.getLoggedErrors()).isEmpty()
    }

    @Test
    fun `launchForm at created launches activity`() = testScenario(
        initialLifecycleState = Lifecycle.State.CREATED,
    ) {
        launchForm("card")

        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("card")
    }

    @Test
    fun `launchForm after destruction leaves state unchanged`() = testScenario {
        selectionHolder.setTemporarySelection("existing")
        val state = selectionHolder.state
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        sheetLauncher.launchForm(
            code = "card",
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = null,
            promotion = null,
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("existing")
        assertThat(selectionHolder.state).isEqualTo(state)
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `launchManage after destruction leaves state unchanged`() = testScenario {
        selectionHolder.setTemporarySelection("existing")
        val state = selectionHolder.state
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        sheetLauncher.launchManage(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = createCustomerState(),
            selection = null,
            configuration = EmbeddedConfigurationFactory.create(),
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("existing")
        assertThat(selectionHolder.state).isEqualTo(state)
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `launchPaymentOptions after destruction leaves state unchanged`() = testScenario {
        selectionHolder.setTemporarySelection("existing")
        val state = selectionHolder.state
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = null,
            selection = null,
            configuration = EmbeddedConfigurationFactory.create(),
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(launcherState.isAwaitingPaymentOptionsReady).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("existing")
        assertThat(selectionHolder.state).isEqualTo(state)
        assertThat(immediateActionWasInvoked()).isFalse()
    }

    @Test
    fun `destruction after successful form launch preserves selection and open state`() = testScenario {
        launchForm("card")
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        assertThat(SheetStateHolder(savedStateHandle).sheetIsOpen).isTrue()
        assertThat(selectionHolder.temporarySelection.value).isEqualTo("card")
    }

    @Suppress("LongMethod")
    private fun testScenario(
        promotions: List<PaymentMethodMessagePromotion>? = null,
        initialLifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        var immediateActionInvoked = false
        val testScope = this
        val lifecycleOwner = TestLifecycleOwner(initialState = initialLifecycleState)
        val savedStateHandle = SavedStateHandle()
        val linkAccountHolder = LinkAccountHolder(savedStateHandle)
        val selectionHolder = CheckoutControllerStateFactory.createStateHolder(
            savedStateHandle = savedStateHandle,
            paymentOptionFactory = DefaultCheckoutPaymentOptionDisplayDataFactory(
                iconLoader = PaymentSelection.IconLoader(
                    resources = applicationContext.resources,
                    imageLoader = FakeStripeImageLoader(),
                ),
                cardArtDrawableLoader = { null },
                context = applicationContext,
                linkAccountHolder = linkAccountHolder,
            ),
        ).apply { state = CheckoutControllerStateFactory.create() }
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val errorReporter = FakeErrorReporter()
        val sessionRefresher = FakeCheckoutSessionRefresher()
        val logger = FakeLogger()
        val confirmationHandler = FakeConfirmationHandler()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = confirmationHandler,
            sheetStateHolder = sheetStateHolder,
            sessionRefresher = sessionRefresher,
            logger = logger,
            resultCallback = {},
            viewModelScope = backgroundScope,
        )
        val launcherState = CheckoutSheetLauncherState(savedStateHandle)
        val embeddedContentState = MutableStateFlow<EmbeddedContentHelperStateHolder.State?>(
            EmbeddedContentHelperStateHolder.State(
                paymentMethodMetadata = paymentMethodMetadata,
                embeddedViewDisplaysMandateText = true,
                configuration = EmbeddedConfigurationFactory.create(),
            )
        )

        DummyActivityResultCaller.test {
            fun createSheetLauncher(
                owner: TestLifecycleOwner,
                state: CheckoutSheetLauncherState,
            ): CheckoutSheetLauncher {
                return CheckoutSheetLauncher(
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = owner,
                    selectionHolder = selectionHolder,
                    customerStateHolder = customerStateHolder,
                    linkAccountHolder = linkAccountHolder,
                    sheetStateHolder = sheetStateHolder,
                    errorReporter = errorReporter,
                    sessionRefresher = sessionRefresher,
                    operationCoordinator = operationCoordinator,
                    launcherState = state,
                    embeddedContentState = embeddedContentState,
                    logger = logger,
                    coroutineScope = testScope,
                    productUsage = setOf("Checkout"),
                    statusBarColor = null,
                    paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
                    rowSelectionImmediateActionHandler = { immediateActionInvoked = true },
                    paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(promotions),
                )
            }

            val sheetLauncher = createSheetLauncher(lifecycleOwner, launcherState)
            val registerCall = awaitRegisterCall()
            val launcher = awaitNextRegisteredLauncher()

            assertThat(registerCall).isNotNull()
            assertThat(registerCall.contract).isInstanceOf<EmbeddedSheetContract>()

            Scenario(
                selectionHolder = selectionHolder,
                lifecycleOwner = lifecycleOwner,
                customerStateHolder = customerStateHolder,
                linkAccountHolder = linkAccountHolder,
                dummyActivityResultCallerScenario = this,
                registerCall = registerCall,
                launcher = launcher,
                sheetLauncher = sheetLauncher,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                immediateActionWasInvoked = { immediateActionInvoked },
                sessionRefresher = sessionRefresher,
                logger = logger,
                operationCoordinator = operationCoordinator,
                launcherState = launcherState,
                savedStateHandle = savedStateHandle,
                embeddedContentState = embeddedContentState,
                coroutineScope = testScope,
                createSheetLauncher = ::createSheetLauncher,
                runCurrent = testScheduler::runCurrent,
            ).block()
        }

        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private class Scenario(
        val selectionHolder: CheckoutControllerStateHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val dummyActivityResultCallerScenario: DummyActivityResultCaller.Scenario,
        val registerCall: RegisterCall<*, *>,
        val launcher: ActivityResultLauncher<*>,
        val sheetLauncher: EmbeddedSheetLauncher,
        val sheetStateHolder: SheetStateHolder,
        val errorReporter: FakeErrorReporter,
        val immediateActionWasInvoked: () -> Boolean,
        val sessionRefresher: FakeCheckoutSessionRefresher,
        val logger: FakeLogger,
        val operationCoordinator: CheckoutOperationCoordinator,
        val launcherState: CheckoutSheetLauncherState,
        val savedStateHandle: SavedStateHandle,
        val embeddedContentState: MutableStateFlow<EmbeddedContentHelperStateHolder.State?>,
        val coroutineScope: CoroutineScope,
        private val createSheetLauncher: (
            TestLifecycleOwner,
            CheckoutSheetLauncherState,
        ) -> CheckoutSheetLauncher,
        private val runCurrent: () -> Unit,
    ) {
        fun runCurrent() {
            runCurrent.invoke()
        }

        fun cancelPaymentOptions(customerState: CustomerState?) {
            registerCall.callback.asCallbackFor<EmbeddedActivityResult>().onActivityResult(
                EmbeddedActivityResult.Cancelled(
                    customerState = customerState,
                    linkAccountInfo = LinkAccountUpdate.Value(null),
                    launchMode = EmbeddedLaunchMode.PaymentOptions,
                )
            )
        }

        fun createLinkPaymentOptionsPresenter() {
            CheckoutLinkPaymentOptionsPresenter(
                defaultPresenter = mock(),
                selectionLauncher = mock(),
                linkPaymentLauncher = mock(),
                activityResultRegistry = mock(),
                lifecycleOwner = lifecycleOwner,
                stateHolder = mock(),
                customerStateHolder = customerStateHolder,
                linkAccountHolder = mock(),
                sheetStateHolder = sheetStateHolder,
            )
        }

        suspend fun recreateSheetLauncher(
            lifecycleOwner: TestLifecycleOwner,
            launcherState: CheckoutSheetLauncherState,
        ) {
            createSheetLauncher(lifecycleOwner, launcherState)
            dummyActivityResultCallerScenario.awaitRegisterCall()
            dummyActivityResultCallerScenario.awaitNextRegisteredLauncher()
        }

        suspend fun awaitRefreshCall(): FakeCheckoutSessionRefresher.Call {
            return sessionRefresher.calls.awaitItem()
        }

        fun expectNoRefreshCalls() {
            sessionRefresher.calls.expectNoEvents()
        }

        suspend fun launchForm(code: String) {
            sheetLauncher.launchForm(
                code = code,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
                configuration = EmbeddedConfigurationFactory.create(),
                customerState = null,
                promotion = null,
            )
            dummyActivityResultCallerScenario.awaitLaunchCall()
        }
    }

    private companion object {
        const val CALLBACK_IDENTIFIER = "CheckoutTestIdentifier"
    }
}
