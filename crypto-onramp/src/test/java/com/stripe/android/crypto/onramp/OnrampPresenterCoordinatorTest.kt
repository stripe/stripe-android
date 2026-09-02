package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.CheckoutState.Status
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutCallback
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodCallback
import com.stripe.android.crypto.onramp.model.OnrampCollectPaymentMethodResult
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.crypto.onramp.model.OnrampStartTermsAndConditionsResult
import com.stripe.android.crypto.onramp.model.OnrampStartTermsOfServiceResult
import com.stripe.android.crypto.onramp.model.OnrampTermsAndConditionsCallback
import com.stripe.android.crypto.onramp.model.OnrampTermsAndConditionsResult
import com.stripe.android.crypto.onramp.model.OnrampTermsOfServiceCallback
import com.stripe.android.crypto.onramp.model.OnrampTermsOfServiceResult
import com.stripe.android.crypto.onramp.model.PaymentMethodSelection
import com.stripe.android.crypto.onramp.model.PaymentMethodType
import com.stripe.android.crypto.onramp.model.SamsungPayAvailabilityResult
import com.stripe.android.crypto.onramp.samsungpay.FakeSamsungPayLauncher
import com.stripe.android.crypto.onramp.samsungpay.FakeSamsungPayLauncherFactory
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayResult
import com.stripe.android.crypto.onramp.samsungpay.SamsungPayStatus
import com.stripe.android.link.LinkController
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnrampPresenterCoordinatorTest {
    private val linkController: LinkController = mock()
    private val interactor: OnrampInteractor = mock()

    private val lifecycleOwner = TestLifecycleOwner()
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
    private val testScope = TestScope()
    private val checkoutCallback = mock<OnrampCheckoutCallback>()
    private val onrampSessionClientSecretProvider: suspend (String) -> String = { "cos_test_secret" }
    private val samsungPayLauncher = FakeSamsungPayLauncher()
    private val samsungPayLauncherFactory = FakeSamsungPayLauncherFactory(samsungPayLauncher)

    @After
    fun tearDown() {
        samsungPayLauncher.ensureAllEventsConsumed()
        samsungPayLauncherFactory.ensureAllEventsConsumed()
    }

    @Test
    fun performCheckout_successfulPayment_callsCallbackWithCompleted() = runTest {
        // Given
        val onrampSessionId = "cos_test_session_id"

        val onrampStateFlow = MutableStateFlow(OnrampState())
        val coordinator = createCoordinator(onrampStateFlow)

        coordinator.performCheckout(onrampSessionId)
        testScope.testScheduler.advanceUntilIdle()

        // Verify startCheckout was called
        verify(interactor).startCheckout(onrampSessionId)

        // Simulate the interactor emitting a completed checkout state (this will trigger the observer)
        onrampStateFlow.value = OnrampState(
            checkoutState = CheckoutState(
                status = Status.Completed(OnrampCheckoutResult.Completed())
            )
        )
        testScope.testScheduler.advanceUntilIdle()

        // Then
        val callbackCaptor = argumentCaptor<OnrampCheckoutResult>()
        verify(checkoutCallback).onResult(callbackCaptor.capture())

        assertThat(callbackCaptor.firstValue)
            .isInstanceOf(OnrampCheckoutResult.Completed::class.java)
    }

    @Test
    fun checkoutStateCompletedCanceled_callsCallbackWithCanceled() = runTest {
        val onrampStateFlow = MutableStateFlow(OnrampState())
        createCoordinator(onrampStateFlow)

        onrampStateFlow.value = OnrampState(
            checkoutState = CheckoutState(
                status = Status.Completed(OnrampCheckoutResult.Canceled())
            )
        )
        testScope.testScheduler.advanceUntilIdle()

        val callbackCaptor = argumentCaptor<OnrampCheckoutResult>()
        verify(checkoutCallback).onResult(callbackCaptor.capture())

        assertThat(callbackCaptor.firstValue)
            .isInstanceOf(OnrampCheckoutResult.Canceled::class.java)
    }

    @Test
    fun checkoutStateCompletedFailed_callsCallbackWithFailed() = runTest {
        val error = RuntimeException("Payment failed")
        val onrampStateFlow = MutableStateFlow(OnrampState())
        createCoordinator(onrampStateFlow)

        onrampStateFlow.value = OnrampState(
            checkoutState = CheckoutState(
                status = Status.Completed(OnrampCheckoutResult.Failed(error))
            )
        )
        testScope.testScheduler.advanceUntilIdle()

        val callbackCaptor = argumentCaptor<OnrampCheckoutResult>()
        verify(checkoutCallback).onResult(callbackCaptor.capture())

        assertThat(callbackCaptor.firstValue)
            .isInstanceOf(OnrampCheckoutResult.Failed::class.java)
        assertThat((callbackCaptor.firstValue as OnrampCheckoutResult.Failed).error)
            .isSameInstanceAs(error)
    }

    @Test
    fun `Samsung Pay configuration creates launcher and reports readiness`() = runTest {
        val expectedAvailability = SamsungPayAvailabilityResult.Available()
        whenever(interactor.handleSamsungPayAvailability(SamsungPayStatus.Ready))
            .thenReturn(expectedAvailability)
        var isReady: Boolean? = null
        var availabilityResult: SamsungPayAvailabilityResult? = null
        val onrampStateFlow = MutableStateFlow(
            OnrampState(configurationState = createConfigurationWithSamsungPay()),
        )

        createCoordinator(
            onrampStateFlow = onrampStateFlow,
            samsungPayIsReadyCallback = { ready, availability ->
                isReady = ready
                availabilityResult = availability
            },
        )
        testScope.testScheduler.advanceUntilIdle()

        val createCall = samsungPayLauncherFactory.createCalls.awaitItem()
        assertThat(createCall.context).isSameInstanceAs(activity.applicationContext)
        assertThat(createCall.configuration.serviceId).isEqualTo("service_123")
        assertThat(createCall.configuration.merchantId).isNull()
        assertThat(createCall.configuration.merchantName).isNull()
        assertThat(createCall.merchantDisplayName).isEqualTo("Stripe merchant")
        assertThat(createCall.configuration.allowedCardBrands)
            .containsExactly(CardBrand.Visa, CardBrand.MasterCard)
            .inOrder()
        samsungPayLauncher.statusCalls.awaitItem()
        assertThat(isReady).isTrue()
        assertThat(availabilityResult).isSameInstanceAs(expectedAvailability)
    }

    @Test
    fun `Samsung Pay unavailable status reports not ready`() = runTest {
        val status = SamsungPayStatus.Failed(IllegalStateException("SDK missing"))
        samsungPayLauncher.status = status
        whenever(interactor.handleSamsungPayAvailability(status)).thenReturn(
            SamsungPayAvailabilityResult.Unavailable(mock()),
        )
        var isReady: Boolean? = null

        createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
            samsungPayIsReadyCallback = { ready, _ -> isReady = ready },
        )
        testScope.testScheduler.advanceUntilIdle()

        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()
        assertThat(isReady).isFalse()
    }

    @Test
    fun `Samsung Pay detailed availability callback receives rich result`() = runTest {
        samsungPayLauncher.status = SamsungPayStatus.NotSupported
        val expectedResult = SamsungPayAvailabilityResult.Unavailable(
            mock(),
        )
        whenever(interactor.handleSamsungPayAvailability(SamsungPayStatus.NotSupported))
            .thenReturn(expectedResult)
        var isReady: Boolean? = null
        var availabilityResult: SamsungPayAvailabilityResult? = null

        createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
            samsungPayIsReadyCallback = { ready, availability ->
                isReady = ready
                availabilityResult = availability
            },
        )
        testScope.testScheduler.advanceUntilIdle()

        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()
        assertThat(isReady).isFalse()
        assertThat(availabilityResult).isSameInstanceAs(expectedResult)
    }

    @Test
    fun `Samsung Pay waits for an Elements session before creating launcher`() = runTest {
        val linkStateFlow = MutableStateFlow(createFakeLinkState(elementsSessionId = null))

        createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
            linkStateFlow = linkStateFlow,
        )
        testScope.testScheduler.advanceUntilIdle()

        samsungPayLauncherFactory.createCalls.expectNoEvents()
        samsungPayLauncher.statusCalls.expectNoEvents()

        linkStateFlow.value = createFakeLinkState(elementsSessionId = "test-elements-session-id")
        testScope.testScheduler.advanceUntilIdle()

        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()
    }

    @Test
    fun `Samsung Pay selection presents and forwards result`() = runTest {
        val samsungPayResult = SamsungPayResult.Canceled
        val collectionResult = OnrampCollectPaymentMethodResult.Cancelled()
        whenever(interactor.getOrFetchPlatformKey()).thenReturn(Result.success("pk_platform_123"))
        whenever(interactor.handleSamsungPayPaymentResult(samsungPayResult, "pk_platform_123"))
            .thenReturn(collectionResult)
        var callbackResult: OnrampCollectPaymentMethodResult? = null
        val coordinator = createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
            collectPaymentCallback = { callbackResult = it },
        )
        testScope.testScheduler.advanceUntilIdle()
        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()

        coordinator.collectPaymentMethod(
            PaymentMethodSelection.SamsungPay(
                currencyCode = "usd",
                amount = 1099,
                orderNumber = "order_123",
            ),
        )

        verify(interactor).onCollectPaymentMethod(PaymentMethodType.SamsungPay)
        testScope.testScheduler.advanceUntilIdle()
        val presentation = samsungPayLauncher.presentCalls.awaitItem()
        assertThat(presentation.currencyCode).isEqualTo("usd")
        assertThat(presentation.amount).isEqualTo(1099)
        assertThat(presentation.orderNumber).isEqualTo("order_123")

        samsungPayLauncher.complete(samsungPayResult)
        testScope.testScheduler.advanceUntilIdle()

        verify(interactor).handleSamsungPayPaymentResult(samsungPayResult, "pk_platform_123")
        assertThat(callbackResult).isSameInstanceAs(collectionResult)
    }

    @Test
    fun `Samsung Pay platform key failure returns failed without presenting`() = runTest {
        val error = IllegalStateException("Platform key unavailable")
        val expectedResult = OnrampCollectPaymentMethodResult.Failed(error)
        whenever(interactor.getOrFetchPlatformKey()).thenReturn(Result.failure(error))
        whenever(interactor.handleSamsungPayPlatformKeyFailure(error)).thenReturn(expectedResult)
        var callbackResult: OnrampCollectPaymentMethodResult? = null
        val coordinator = createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
            collectPaymentCallback = { callbackResult = it },
        )
        testScope.testScheduler.advanceUntilIdle()
        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()

        coordinator.collectPaymentMethod(
            PaymentMethodSelection.SamsungPay(
                currencyCode = "usd",
                amount = 1099,
                orderNumber = "order_123",
            ),
        )
        testScope.testScheduler.advanceUntilIdle()

        assertThat(callbackResult).isSameInstanceAs(expectedResult)
        verify(interactor).handleSamsungPayPlatformKeyFailure(error)
        samsungPayLauncher.presentCalls.expectNoEvents()
    }

    @Test
    fun `destroying lifecycle destroys Samsung Pay launcher`() = runTest {
        createCoordinator(
            onrampStateFlow = MutableStateFlow(
                OnrampState(configurationState = createConfigurationWithSamsungPay()),
            ),
        )
        testScope.testScheduler.advanceUntilIdle()
        samsungPayLauncherFactory.createCalls.awaitItem()
        samsungPayLauncher.statusCalls.awaitItem()

        lifecycleOwner.currentState = Lifecycle.State.DESTROYED

        samsungPayLauncher.destroyCalls.awaitItem()
    }

    @Test
    fun `terms not required invokes callback without presenting`() = runTest {
        whenever(interactor.startTermsAndConditions()).thenReturn(
            OnrampStartTermsAndConditionsResult.NotRequired
        )
        val results = Turbine<OnrampTermsAndConditionsResult>()
        val coordinator = createCoordinator(
            termsAndConditionsCallback = OnrampTermsAndConditionsCallback(results::add),
        )

        coordinator.presentTermsAndConditionsIfNeeded()
        testScope.testScheduler.advanceUntilIdle()

        assertThat(results.awaitItem())
            .isInstanceOf(OnrampTermsAndConditionsResult.NotRequired::class.java)
        results.ensureAllEventsConsumed()
    }

    @Test
    fun `terms of service not required invokes callback without presenting`() = runTest {
        whenever(interactor.startTermsOfService()).thenReturn(
            OnrampStartTermsOfServiceResult.NotRequired
        )
        val results = Turbine<OnrampTermsOfServiceResult>()
        val coordinator = createCoordinator(
            termsOfServiceCallback = OnrampTermsOfServiceCallback(results::add),
        )

        coordinator.presentTermsOfServiceIfNeeded()
        testScope.testScheduler.advanceUntilIdle()

        assertThat(results.awaitItem())
            .isInstanceOf(OnrampTermsOfServiceResult.NotRequired::class.java)
        results.ensureAllEventsConsumed()
    }

    private fun createCoordinator(
        onrampStateFlow: MutableStateFlow<OnrampState> = MutableStateFlow(OnrampState()),
        linkStateFlow: MutableStateFlow<LinkController.State> = MutableStateFlow(createFakeLinkState()),
        samsungPayIsReadyCallback: ((Boolean, SamsungPayAvailabilityResult) -> Unit)? = null,
        collectPaymentCallback: OnrampCollectPaymentMethodCallback = OnrampCollectPaymentMethodCallback {},
        termsAndConditionsCallback: OnrampTermsAndConditionsCallback? = null,
        termsOfServiceCallback: OnrampTermsOfServiceCallback? = null,
    ): OnrampPresenterCoordinator {
        lifecycleOwner.currentState = Lifecycle.State.STARTED

        val linkPresenter = mock<LinkController.Presenter>()

        whenever(linkController.state(any())).thenReturn(linkStateFlow)
        whenever(
            linkController.createPresenter(
                activity = any(),
                presentPaymentMethodsCallback = any(),
                authenticationCallback = any(),
                authorizeCallback = any(),
                presentCallback = any(),
                confirmSetupIntentCallback = any(),
            )
        ).thenReturn(linkPresenter)

        whenever(interactor.state).thenReturn(onrampStateFlow)

        val callbacks = OnrampCallbacks()
            .checkoutCallback(checkoutCallback)
            .verifyIdentityCallback {}
            .collectPaymentCallback(collectPaymentCallback)
            .authorizeCallback {}
            .verifyKycCallback {}
            .onrampSessionClientSecretProvider(onrampSessionClientSecretProvider)

        samsungPayIsReadyCallback?.let(callbacks::samsungPayIsReadyCallback)
        termsAndConditionsCallback?.let(callbacks::termsAndConditionsCallback)
        termsOfServiceCallback?.let(callbacks::termsOfServiceCallback)

        OnrampCallbackReferences[DEFAULT_ONRAMP_INSTANCE_KEY] = callbacks.build()

        return OnrampPresenterCoordinator(
            linkController = linkController,
            interactor = interactor,
            lifecycleOwner = lifecycleOwner,
            activity = activity,
            coroutineScope = testScope,
            onrampCallbackIdentifier = DEFAULT_ONRAMP_INSTANCE_KEY,
            samsungPayLauncherFactory = samsungPayLauncherFactory,
        )
    }

    private fun createConfigurationWithSamsungPay(): OnrampConfiguration.State {
        return OnrampConfiguration()
            .merchantDisplayName("Stripe merchant")
            .publishableKey("pk_test_123")
            .samsungPayConfig(
                OnrampConfiguration.SamsungPayConfig(
                    serviceId = "service_123",
                    allowedCardBrands = listOf(CardBrand.Visa, CardBrand.MasterCard),
                ),
            )
            .build()
    }

    private fun createFakeLinkState(
        elementsSessionId: String? = "test-elements-session-id",
    ): LinkController.State {
        return LinkController.State(
            internalLinkAccount = null,
            merchantLogoUrl = null,
            selectedPaymentMethodPreview = null,
            createdPaymentMethod = null,
            elementsSessionId = elementsSessionId,
        )
    }
}
