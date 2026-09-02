package com.stripe.android.paymentelement.embedded.content

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
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.asCallbackFor
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.previousNewSelection
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedSheetContract
import com.stripe.android.paymentelement.embedded.stashNewSelection
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.DummyActivityResultCaller.RegisterCall
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Suppress("LargeClass")
@RunWith(RobolectricTestRunner::class)
internal class DefaultEmbeddedSheetLauncherTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Application>()

    private val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(coroutineScopeCleanupRule)
        .around(PaymentConfigurationTestRule(applicationContext))

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
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
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
    fun `launchForm launches activity with correct current selection if selection matches`() = testScenario {
        val code = "card"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        selectionHolder.setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `launchForm launches activity with correct current selection if selection is saved card`() = testScenario {
        val code = "card"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        selectionHolder.setSelection(PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD))
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isNull()
    }

    @Test
    fun `launchForm launches activity with correct current selection if selection is for another LPM`() = testScenario {
        val code = "card"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.selection).isNull()
    }

    @Test
    fun `launchForm logs error and returns if confirmation state is null`() = testScenario {
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
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
        val code = "test_code"
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchForm(
            code = code,
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            customerState = createCustomerState(),
            promotion = null,
        )
    }

    @Test
    fun `formActivityLauncher clears selection holder and invokes callback on complete result`() = testScenario {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.setSelection(selection)
        launchForm("test_code")

        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            selection = null,
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
        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
    }

    @Test
    fun `formActivityLauncher sets selection holder on complete result`() = testScenario(
        shouldRowSelectionBeInvoked = true
    ) {
        val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
        selectionHolder.setSelection(selection)
        launchForm("cashapp")

        val result = EmbeddedActivityResult.Complete(
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
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        assertThat(selectionHolder.getPreviousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
    }

    @Test
    fun `formActivityLauncher invokes rowSelectionCallback on complete result when formSheetAction continue`() {
        testScenario(
            shouldRowSelectionBeInvoked = true
        ) {
            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionHolder.setSelection(selection)
            launchForm("cashapp")

            val result = EmbeddedActivityResult.Complete(
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
        }
    }

    @Test
    fun `formActivityLauncher doesn't invokes rowSelectionCallback on complete result when formSheetAction confirm`() {
        testScenario(
            shouldRowSelectionBeInvoked = false
        ) {
            val selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION
            selectionHolder.setSelection(selection)
            launchForm("cashapp")

            val result = EmbeddedActivityResult.Complete(
                previousNewSelections = Bundle(),
                selection = PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION,
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
            assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
            assertThat(
                callbackHelper.callbackTurbine.awaitItem()
            ).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
        }
    }

    @Test
    fun `formActivityLauncher callback does not update selection holder on non-complete result`() = testScenario {
        launchForm("test_code")

        val result = EmbeddedActivityResult.Cancelled(
            customerState = null,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(callbackHelper.callbackTurbine.awaitItem())
            .isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
    }

    @Test
    fun `formActivityLauncher callback does not invoke rowSelectionCallback on non-complete result`() {
        testScenario(
            shouldRowSelectionBeInvoked = false
        ) {
            launchForm("test_code")

            val result = EmbeddedActivityResult.Cancelled(
                customerState = null,
                launchMode = EmbeddedLaunchMode.Form(
                    selectedPaymentMethodCode = "card",
                ),
            )
            val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

            callback.onActivityResult(result)
            assertThat(callbackHelper.callbackTurbine.awaitItem())
                .isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
        }
    }

    @Test
    fun `formActivityLauncher callback sets customer state when available on complete`() = testScenario {
        launchForm("test_code")

        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = createCustomerState(),
            selection = null,
            hasBeenConfirmed = false,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
    }

    @Test
    fun `formActivityLauncher callback sets customer state when available on cancel`() = testScenario {
        launchForm("test_code")

        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val result = EmbeddedActivityResult.Cancelled(
            customerState = createCustomerState(),
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)
        assertThat(customerStateHolder.customer.value).isEqualTo(customerState)
        assertThat(callbackHelper.callbackTurbine.awaitItem())
            .isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
    }

    @Test
    fun `launchManage launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
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
    fun `launchManage is not launched again when the sheet is already open`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchManage(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
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
    }

    @Test
    fun `manageSheetLauncher callback invokes rowSelectionCallback when flag set`() {
        testScenario(
            shouldRowSelectionBeInvoked = true
        ) {
            sheetStateHolder.sheetIsOpen = true
            val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            val result = EmbeddedActivityResult.Complete(
                previousNewSelections = Bundle(),
                customerState = customerState,
                selection = selection,
                hasBeenConfirmed = false,
                checkoutSessionResponse = null,
                shouldInvokeSelectionCallback = true,
                launchMode = EmbeddedLaunchMode.Manage,
            )

            val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
            callback.onActivityResult(result)
        }
    }

    @Test
    fun `manageSheetLauncher callback doesn't invokes rowSelectionCallback when flag not set`() {
        testScenario(
            shouldRowSelectionBeInvoked = false
        ) {
            sheetStateHolder.sheetIsOpen = true
            val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
            val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            val result = EmbeddedActivityResult.Complete(
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
        }
    }

    @Test
    fun `manageSheetLauncher callback does not update state on error result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        val result = EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.Manage)
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(customerStateHolder.customer.value).isEqualTo(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
        assertThat(selectionHolder.selection.value).isEqualTo(null)
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `manageSheetLauncher callback does not invoke rowSelectionCallback on error result`() {
        testScenario(
            shouldRowSelectionBeInvoked = false
        ) {
            sheetStateHolder.sheetIsOpen = true
            customerStateHolder.setCustomerState(PaymentSheetFixtures.EMPTY_CUSTOMER_STATE)
            val result = EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.Manage)
            val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

            callback.onActivityResult(result)
        }
    }

    @Test
    fun `form result handled correctly without prior launchForm call (simulates host recreation)`() = testScenario {
        val result = EmbeddedActivityResult.Complete(
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

        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
    }

    @Test
    fun `form cancellation handled correctly without prior launchForm call`() = testScenario {
        val result = EmbeddedActivityResult.Cancelled(
            customerState = null,
            launchMode = EmbeddedLaunchMode.Form(
                selectedPaymentMethodCode = "card",
            ),
        )
        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()

        callback.onActivityResult(result)

        assertThat(selectionHolder.temporarySelection.value).isNull()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Canceled>()
    }

    @Test
    fun `launchPaymentOptions launches activity with correct parameters`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val selection = PaymentSelection.GooglePay
        val expectedArgs = EmbeddedActivityArgs(
            paymentMethodMetadata = paymentMethodMetadata,
            configuration = EmbeddedConfigurationFactory.create(),
            productUsage = setOf("EmbeddedPaymentElement"),
            paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
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
    fun `launchLoading launches activity with Loading mode`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
        val configuration = EmbeddedConfigurationFactory.create()

        sheetLauncher.launchLoading(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = customerState,
            selection = PaymentSelection.GooglePay,
            configuration = configuration,
        )

        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs
        assertThat(launchCall.paymentMethodMetadata).isEqualTo(paymentMethodMetadata)
        assertThat(launchCall.customerState).isEqualTo(customerState)
        assertThat(launchCall.selection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(launchCall.configuration).isEqualTo(configuration)
        assertThat(launchCall.launchMode).isEqualTo(EmbeddedLaunchMode.Loading)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Test
    fun `launchPaymentOptions logs error and returns if confirmation state is null`() = testScenario {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = paymentMethodMetadata,
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
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        sheetStateHolder.sheetIsOpen = true
        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = paymentMethodMetadata,
            customerState = null,
            selection = null,
            configuration = EmbeddedConfigurationFactory.create(),
        )
    }

    @Test
    fun `launchPaymentOptions forwards previously entered new selections`() = testScenario {
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)

        sheetLauncher.launchPaymentOptions(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
            selection = PaymentSelection.GooglePay,
            configuration = EmbeddedConfigurationFactory.create(),
        )
        val launchCall = dummyActivityResultCallerScenario.awaitLaunchCall() as EmbeddedActivityArgs

        assertThat(launchCall.previousNewSelections.previousNewSelection("cashapp"))
            .isEqualTo(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
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
    fun `paymentOptionsResult callback invokes completion callback on confirmed result`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        val result = EmbeddedActivityResult.Complete(
            previousNewSelections = Bundle(),
            customerState = null,
            selection = null,
            hasBeenConfirmed = true,
            checkoutSessionResponse = null,
            shouldInvokeSelectionCallback = false,
            launchMode = EmbeddedLaunchMode.PaymentOptions,
        )

        val callback = registerCall.callback.asCallbackFor<EmbeddedActivityResult>()
        callback.onActivityResult(result)

        assertThat(callbackHelper.stateHelper.stateTurbine.awaitItem()).isNull()
        assertThat(callbackHelper.callbackTurbine.awaitItem()).isInstanceOf<EmbeddedPaymentElement.Result.Completed>()
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
        val savedSelection = PaymentSelection.Saved(paymentMethod)
        selectionHolder.setSelection(savedSelection)
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
    fun `onDestroy unregisters launcher`() = testScenario {
        sheetStateHolder.sheetIsOpen = true
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val unregisteredLauncher = dummyActivityResultCallerScenario.awaitNextUnregisteredLauncher()

        assertThat(unregisteredLauncher).isEqualTo(launcher)
        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
    }

    @Suppress("LongMethod")
    private fun testScenario(
        shouldRowSelectionBeInvoked: Boolean = false,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        var rowSelectionCallbackInvoked = false
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
        val stateHelper = FakeEmbeddedStateHelper()
        val callbackHelper = FakeEmbeddedResultCallbackHelper(
            stateHelper = stateHelper
        )
        val immediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = coroutineScopeCleanupRule.track(CoroutineScope(UnconfinedTestDispatcher())),
            internalRowSelectionCallback = { { rowSelectionCallbackInvoked = true } }
        )

        DummyActivityResultCaller.test {
            val sheetLauncher = DefaultEmbeddedSheetLauncher(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                sheetStateHolder = sheetStateHolder,
                errorReporter = errorReporter,
                productUsage = setOf("EmbeddedPaymentElement"),
                statusBarColor = null,
                paymentElementCallbackIdentifier = "EmbeddedFormTestIdentifier",
                embeddedResultCallbackHelper = callbackHelper,
                rowSelectionImmediateActionHandler = immediateActionHandler,
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
                callbackHelper = callbackHelper,
            ).block()

            assertThat(shouldRowSelectionBeInvoked).isEqualTo(rowSelectionCallbackInvoked)

            callbackHelper.validate()
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
        val errorReporter: FakeErrorReporter,
        val callbackHelper: FakeEmbeddedResultCallbackHelper,
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
}
