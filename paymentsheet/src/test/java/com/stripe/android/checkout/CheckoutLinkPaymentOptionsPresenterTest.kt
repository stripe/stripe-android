@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.injection.CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.LinkPaymentMethodSelectionLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionResultHandler
import com.stripe.android.link.TestFactory
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.createCustomerState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.verify

internal class CheckoutLinkPaymentOptionsPresenterTest {
    @Test
    fun `registers direct Link with unique key and host registry`() = runScenario {
        verify(linkPaymentLauncher).register(
            key = eq(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER),
            activityResultRegistry = same(activityResultRegistry),
            callback = any(),
        )
    }

    @Test
    fun `ordinary payment options delegate to default presenter`() = runScenario(
        selection = PaymentSelection.GooglePay,
    ) {
        presenter.present()

        verify(defaultPresenter).present()
        verifyLinkWasNotPresented()
    }

    @Test
    fun `eligible Link launches with exact arguments and keeps sheet open`() = runScenario {
        presenter.present()

        assertThat(sheetStateHolder.sheetIsOpen).isTrue()
        verify(linkPaymentLauncher).present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment.details),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = 123,
        )
        verify(defaultPresenter, never()).present()
    }

    @Test
    fun `completion updates selection and closes`() = runScenario {
        presenter.present()
        val updatedPayment = selectedPayment.copy(collectedCvc = "123")

        resultCallback(
            LinkActivityResult.Completed(LinkAccountUpdate.None, selectedPayment = updatedPayment)
        )

        assertThat(stateHolder.state?.paymentSelection)
            .isEqualTo(linkSelection.copy(selectedPayment = updatedPayment))
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Test
    fun `logout uses current customer fallback and opens payment options`() = runScenario {
        presenter.present()
        val fallback = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        customerState.value = createCustomerState(paymentMethods = listOf(fallback))

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.LoggedOut,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(stateHolder.state?.paymentSelection).isEqualTo(PaymentSelection.Saved(fallback))
        verify(defaultPresenter).present()
    }

    @Test
    fun `pay another way opens payment options`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.PayAnotherWay,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        verify(defaultPresenter).present()
    }

    @Test
    fun `back opens payment options when Link has no usable payment`() = runScenario(
        selection = linkSelection.copy(selectedPayment = null),
    ) {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        verify(defaultPresenter).present()
    }

    @Test
    fun `back closes when Link has a usable payment`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Canceled(
                reason = LinkActivityResult.Canceled.Reason.BackPressed,
                linkAccountUpdate = LinkAccountUpdate.None,
            )
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        verify(defaultPresenter, never()).present()
    }

    @Test
    fun `failure closes and applies Link account update`() = runScenario {
        presenter.present()

        resultCallback(
            LinkActivityResult.Failed(
                error = Throwable("failure"),
                linkAccountUpdate = LinkAccountUpdate.Value(null),
            )
        )

        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
        assertThat(linkAccountHolder.linkAccountInfo.value.account).isNull()
        assertThat(stateHolder.state?.paymentMethodMetadata?.linkState?.loginState)
            .isEqualTo(com.stripe.android.paymentsheet.state.LinkState.LoginState.LoggedOut)
    }

    @Test
    fun `verification dismissal persists suppression across recreation`() {
        val savedStateHandle = SavedStateHandle()
        runScenario(savedStateHandle = savedStateHandle) {
            presenter.present()
            val verifyingAccount = mock<LinkAccount> {
                on { accountStatus } doReturn AccountStatus.VerificationStarted
            }

            resultCallback(
                LinkActivityResult.Canceled(
                    reason = LinkActivityResult.Canceled.Reason.BackPressed,
                    linkAccountUpdate = LinkAccountUpdate.Value(verifyingAccount),
                )
            )

            assertThat(stateHolder.state?.linkEagerPresentationSuppressed).isTrue()
        }

        runScenario(savedStateHandle = savedStateHandle) {
            sheetStateHolder.sheetIsOpen = false
            presenter.present()

            verify(defaultPresenter).present()
            verifyLinkWasNotPresented()
        }
    }

    @Test
    fun `destroy unregisters direct Link and clears open state`() = runScenario {
        presenter.present()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(linkPaymentLauncher).unregister()
        assertThat(sheetStateHolder.sheetIsOpen).isFalse()
    }

    @Suppress("LongMethod")
    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        selection: PaymentSelection? = linkSelection,
        block: Scenario.() -> Unit,
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            linkState = com.stripe.android.paymentsheet.state.LinkState(
                configuration = TestFactory.LINK_CONFIGURATION,
                loginState = com.stripe.android.paymentsheet.state.LinkState.LoginState.LoggedIn,
                signupMode = null,
            )
        )
        val existingState = savedStateHandle.get<CheckoutControllerState>(CheckoutControllerStateHolder.STATE_KEY)
        val state = existingState ?: CheckoutControllerStateFactory.create(
            paymentSelection = selection,
            paymentMethodMetadata = paymentMethodMetadata,
        )
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle).apply {
            this.state = state
        }
        val customerState = MutableStateFlow<CustomerState?>(null)
        val customerStateHolder = mock<CustomerStateHolder> {
            on { customer } doReturn customerState
        }
        val linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT)
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle()).apply { set(linkAccountInfo) }
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val defaultPresenter = mock<DefaultEmbeddedPaymentOptionsPresenter>()
        val linkPaymentLauncher = mock<LinkPaymentLauncher>()
        val activityResultRegistry = mock<ActivityResultRegistry>()
        val lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val presenter = CheckoutLinkPaymentOptionsPresenter(
            defaultPresenter = defaultPresenter,
            selectionLauncher = LinkPaymentMethodSelectionLauncher(FakeLinkGate.Factory()),
            resultHandler = LinkPaymentMethodSelectionResultHandler(),
            linkPaymentLauncher = linkPaymentLauncher,
            activityResultRegistry = activityResultRegistry,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerStateHolder = customerStateHolder,
            linkAccountHolder = linkAccountHolder,
            sheetStateHolder = sheetStateHolder,
            statusBarColor = 123,
        )
        val callbackCaptor = argumentCaptor<(LinkActivityResult) -> Unit>()
        verify(linkPaymentLauncher).register(
            key = eq(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER),
            activityResultRegistry = same(activityResultRegistry),
            callback = callbackCaptor.capture(),
        )

        Scenario(
            presenter = presenter,
            defaultPresenter = defaultPresenter,
            linkPaymentLauncher = linkPaymentLauncher,
            activityResultRegistry = activityResultRegistry,
            lifecycleOwner = lifecycleOwner,
            stateHolder = stateHolder,
            customerState = customerState,
            sheetStateHolder = sheetStateHolder,
            linkAccountHolder = linkAccountHolder,
            linkAccountInfo = linkAccountInfo,
            paymentMethodMetadata = paymentMethodMetadata,
            resultCallback = callbackCaptor.firstValue,
        ).block()
    }

    private data class Scenario(
        val presenter: CheckoutLinkPaymentOptionsPresenter,
        val defaultPresenter: DefaultEmbeddedPaymentOptionsPresenter,
        val linkPaymentLauncher: LinkPaymentLauncher,
        val activityResultRegistry: ActivityResultRegistry,
        val lifecycleOwner: TestLifecycleOwner,
        val stateHolder: CheckoutControllerStateHolder,
        val customerState: MutableStateFlow<CustomerState?>,
        val sheetStateHolder: SheetStateHolder,
        val linkAccountHolder: LinkAccountHolder,
        val linkAccountInfo: LinkAccountUpdate.Value,
        val paymentMethodMetadata: com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata,
        val resultCallback: (LinkActivityResult) -> Unit,
    ) {
        val linkConfiguration: LinkConfiguration
            get() = requireNotNull(paymentMethodMetadata.linkState?.configuration)

        fun verifyLinkWasNotPresented() {
            verify(linkPaymentLauncher, never()).present(
                configuration = any(),
                paymentMethodMetadata = any(),
                linkAccountInfo = any(),
                launchMode = any(),
                linkExpressMode = any(),
                statusBarColor = anyOrNull(),
            )
        }
    }

    private companion object {
        val selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
            details = TestFactory.CONSUMER_PAYMENT_DETAILS_CARD,
            collectedCvc = null,
            billingPhone = null,
        )
        val linkSelection = PaymentSelection.Link(
            brand = LinkBrand.Link,
            selectedPayment = selectedPayment,
        )
    }
}
