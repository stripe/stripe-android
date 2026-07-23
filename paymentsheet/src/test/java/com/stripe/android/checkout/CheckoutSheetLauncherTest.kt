package com.stripe.android.checkout

import android.app.Application
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
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationFactory
import com.stripe.android.paymentelement.embedded.content.EmbeddedSheetLauncher
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.DummyActivityResultCaller.RegisterCall
import com.stripe.android.testing.FakeErrorReporter
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
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = null,
            customerState = customerState,
            promotion = promotion,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = code),
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
            selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
            hasBeenConfirmed = false,
            customerState = customerState,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = null,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
    }

    @Test
    fun `formActivityLauncher sets customer state but keeps selection on cancelled result`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        launchForm("card")

        val customerState = createCustomerState()
        val result = EmbeddedActivityResult.Cancelled(
            customerState = customerState,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
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
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
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
            selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            hasBeenConfirmed = true,
            customerState = null,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = null,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `complete result with checkout session response reloads state`() = testScenario {
        val initialResponse = CheckoutSessionResponseFactory.create()
        stateHolder.state = CheckoutControllerStateFactory.create(checkoutSessionResponse = initialResponse)
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        val confirmedResponse = initialResponse.copy(id = "cs_confirmed")
        val result = EmbeddedActivityResult.Complete(
            selection = null,
            hasBeenConfirmed = true,
            customerState = null,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = confirmedResponse,
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(checkoutStateLoader.reloadCalls.awaitItem().checkoutSessionResponse)
            .isEqualTo(confirmedResponse)
        // The reload re-derives the selection, so the manual selection update is skipped.
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        checkoutStateLoader.ensureAllEventsConsumed()
    }

    @Test
    fun `complete result with checkout session response is ignored when no state is loaded`() = testScenario {
        val result = EmbeddedActivityResult.Complete(
            selection = null,
            hasBeenConfirmed = true,
            customerState = null,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        checkoutStateLoader.reloadCalls.expectNoEvents()
        checkoutStateLoader.ensureAllEventsConsumed()
    }

    @Test
    fun `complete result reports error when reload fails`() = testScenario(
        reloadResult = Result.failure(RuntimeException("boom")),
    ) {
        stateHolder.state = CheckoutControllerStateFactory.create()
        val result = EmbeddedActivityResult.Complete(
            selection = null,
            hasBeenConfirmed = true,
            customerState = null,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = CheckoutSessionResponseFactory.create(),
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        // Draining the reload call runs the coroutine through its failure handling.
        checkoutStateLoader.reloadCalls.awaitItem()
        assertThat(errorReporter.getLoggedErrors())
            .contains("unexpected_error.checkout.reload_after_confirm_failed")
        checkoutStateLoader.ensureAllEventsConsumed()
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = PaymentSelection.GooglePay,
            customerState = customerState,
            promotion = null,
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
            customerState = customerState,
            selection = selection,
            hasBeenConfirmed = false,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = null,
            launchMode = EmbeddedLaunchMode.Manage,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
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
            paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
            statusBarColor = null,
            selection = selection,
            customerState = customerState,
            promotion = null,
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
    fun `paymentOptionsResult callback updates state on complete result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = EmbeddedActivityResult.Complete(
            customerState = customerState,
            selection = selection,
            hasBeenConfirmed = false,
            shouldInvokeSelectionCallback = false,
            checkoutSessionResponse = null,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(selectionHolder.selection.value).isEqualTo(selection)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
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
        val result = EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.PaymentOptions)
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

    private fun testScenario(
        reloadResult: Result<Unit> = Result.success(Unit),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val backgroundScope = this.backgroundScope
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
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        val checkoutStateLoader = FakeCheckoutStateLoader(reloadResult = reloadResult)
        val errorReporter = FakeErrorReporter()

        DummyActivityResultCaller.test {
            val sheetLauncher = CheckoutSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
                stateHolder = stateHolder,
                checkoutStateLoader = checkoutStateLoader,
                errorReporter = errorReporter,
                coroutineScope = backgroundScope,
                statusBarColor = null,
                paymentElementCallbackIdentifier = CALLBACK_IDENTIFIER,
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
                stateHolder = stateHolder,
                checkoutStateLoader = checkoutStateLoader,
                errorReporter = errorReporter,
            ).block()
        }
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
        val stateHolder: CheckoutControllerStateHolder,
        val checkoutStateLoader: FakeCheckoutStateLoader,
        val errorReporter: FakeErrorReporter,
    ) {
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
