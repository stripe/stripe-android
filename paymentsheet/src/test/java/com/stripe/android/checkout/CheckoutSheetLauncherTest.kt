package com.stripe.android.checkout

import android.app.Application
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodMessageLearnMore
import com.stripe.android.model.PaymentMethodMessagePromotion
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationFactory
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
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.DummyActivityResultCaller.RegisterCall
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.asCallbackFor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(CheckoutSessionPreview::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
internal class CheckoutSheetLauncherTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val paymentConfigurationTestRule = PaymentConfigurationTestRule(applicationContext)

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
            promotions = listOf(promotion),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = code,
            ),
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = customerState,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = true,
            customerState = null,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = null,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            hasBeenConfirmed = true,
            customerState = null,
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
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.Manage,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            customerState = customerState,
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
            temporarySelection = null,
            previousNewSelections = Bundle(),
            customerState = null,
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
    fun `manageSheetLauncher callback does not update state on cancelled result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)),
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
    fun `launchPaymentOptions launches activity with correct parameters`() = testScenario {
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
            promotions = emptyList(),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
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
            temporarySelection = null,
            previousNewSelections = returnedSelections,
            customerState = null,
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
        val result = EmbeddedActivityResult.Complete(
            temporarySelection = null,
            previousNewSelections = Bundle(),
            customerState = customerState,
            selection = selection,
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult contains checkout session refresh failure`() = testScenario {
        val response = CheckoutSessionResponseFactory.create()
        val expectedError = IllegalStateException("Refresh failed")
        sessionRefresher.enqueueRefreshAction { throw expectedError }
        val result = EmbeddedActivityResult.Complete(
            temporarySelection = null,
            previousNewSelections = Bundle(),
            customerState = null,
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
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult cancelled clears stale saved selection`() = testScenario {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        selectionHolder.setSelection(PaymentSelection.Saved(paymentMethod))
        customerStateHolder.setCustomerState(createCustomerState(paymentMethods = listOf(paymentMethod)))

        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(paymentMethods = emptyList()),
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(selectionHolder.selection.value).isNull()
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
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(selectionHolder.selection.value).isEqualTo(savedSelection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `paymentOptionsResult does not update state on error result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = EmbeddedActivityResult.Error(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
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

    @Suppress("LongMethod")
    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        var immediateActionInvoked = false
        val testScope = this
        val lifecycleOwner = TestLifecycleOwner()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = DefaultEmbeddedSelectionHolder(savedStateHandle)
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
            resultCallback = CheckoutController.ResultCallback {},
        )

        DummyActivityResultCaller.test {
            val sheetLauncher = CheckoutSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                sessionRefresher = sessionRefresher,
                operationCoordinator = operationCoordinator,
                logger = logger,
                coroutineScope = testScope,
                productUsage = setOf("Checkout"),
                statusBarColor = null,
                paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
                rowSelectionImmediateActionHandler = { immediateActionInvoked = true },
            )
            val registerCall = awaitRegisterCall()
            val launcher = awaitNextRegisteredLauncher()

            assertThat(registerCall).isNotNull()
            assertThat(registerCall.contract).isInstanceOf<EmbeddedSheetContract>()

            Scenario(
                selectionHolder = selectionHolder,
                lifecycleOwner = lifecycleOwner,
                customerStateHolder = customerStateHolder,
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
                runCurrent = testScheduler::runCurrent,
            ).block()
        }

        confirmationHandler.validate()
        sessionRefresher.ensureAllEventsConsumed()
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val lifecycleOwner: TestLifecycleOwner,
        val customerStateHolder: CustomerStateHolder,
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
        private val runCurrent: () -> Unit,
    ) {
        fun runCurrent() {
            runCurrent.invoke()
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
