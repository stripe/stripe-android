package com.stripe.android.crypto.onramp

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.model.GetOnrampSessionResponse
import com.stripe.android.crypto.onramp.model.GetPlatformSettingsResponse
import com.stripe.android.crypto.onramp.model.OnrampCallbacks
import com.stripe.android.crypto.onramp.model.OnrampCheckoutCallback
import com.stripe.android.crypto.onramp.model.OnrampCheckoutResult
import com.stripe.android.crypto.onramp.repositories.CryptoApiRepository
import com.stripe.android.link.LinkController
import com.stripe.android.model.StripeIntent.Status.Succeeded
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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
    private val cryptoApiRepository: CryptoApiRepository = mock()
    private val lifecycleOwner = TestLifecycleOwner()
    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().get()
    private val testScope = TestScope()
    private val checkoutCallback = mock<OnrampCheckoutCallback>()

    private fun createFakeLinkState(): LinkController.State {
        return LinkController.State(
            internalLinkAccount = null,
            merchantLogoUrl = null,
            selectedPaymentMethodPreview = null,
            createdPaymentMethod = null
        )
    }

    @Test
    fun performCheckout_successfulPayment_callsCallbackWithCompleted() = runTest {
        // Given
        val onrampSessionId = "cos_test_session_id"
        val sessionClientSecret = "cos_test_secret"
        val platformApiKey = "pk_test_platform_key"
        val paymentIntentClientSecret = "pi_test_secret"

        val platformSettings = GetPlatformSettingsResponse(publishableKey = platformApiKey)
        val onrampSession = GetOnrampSessionResponse(
            id = onrampSessionId,
            clientSecret = sessionClientSecret,
            paymentIntentClientSecret = paymentIntentClientSecret
        )
        val succeededPaymentIntent = paymentIntent(
            status = Succeeded
        )

        whenever(cryptoApiRepository.getPlatformSettings())
            .thenReturn(Result.success(platformSettings))

        whenever(cryptoApiRepository.getOnrampSession(onrampSessionId, sessionClientSecret))
            .thenReturn(Result.success(onrampSession))

        whenever(cryptoApiRepository.retrievePaymentIntent(paymentIntentClientSecret, platformApiKey))
            .thenReturn(Result.success(succeededPaymentIntent))

        val onrampSessionClientSecretProvider: suspend () -> String = { sessionClientSecret }

        // When
        val coordinator = createCoordinator()
        coordinator.performCheckout(onrampSessionId, onrampSessionClientSecretProvider)

        testScope.testScheduler.advanceUntilIdle()

        // Then
        val callbackCaptor = argumentCaptor<OnrampCheckoutResult>()
        verify(checkoutCallback).onResult(callbackCaptor.capture())

        assertThat(callbackCaptor.firstValue)
            .isEqualTo(OnrampCheckoutResult.Completed)
    }

    private fun createCoordinator(): OnrampPresenterCoordinator {
        lifecycleOwner.currentState = Lifecycle.State.STARTED

        val linkState = createFakeLinkState()
        val linkStateFlow = MutableStateFlow(linkState)
        val onrampState = OnrampState(
            configuration = null,
            linkControllerState = null
        )
        val onrampStateFlow = MutableStateFlow(onrampState)
        val linkPresenter = mock<LinkController.Presenter>()

        whenever(linkController.state(any())).thenReturn(linkStateFlow)
        whenever(
            linkController.createPresenter(
                activity = any(),
                presentPaymentMethodsCallback = any(),
                authenticationCallback = any(),
                authorizeCallback = any()
            )
        ).thenReturn(linkPresenter)
        whenever(interactor.state).thenReturn(onrampStateFlow)

        return OnrampPresenterCoordinator(
            linkController = linkController,
            interactor = interactor,
            cryptoApiRepository = cryptoApiRepository,
            lifecycleOwner = lifecycleOwner,
            activity = activity,
            onrampCallbacks = OnrampCallbacks(
                checkoutCallback = checkoutCallback,
                identityVerificationCallback = { },
                authenticationCallback = {},
                selectPaymentCallback = {}
            ),
            coroutineScope = testScope
        )
    }
}
