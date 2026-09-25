package com.stripe.android.paymentsheet

import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.utils.FakeActivityResultLauncher
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetLauncherTest {
    private val callbackIdentifier = Identifiable()

    @get:Rule
    val callbackTestRule = PaymentElementCallbackTestRule()

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `init and present should return expected PaymentResult`() {
        val testRegistry = FakeActivityResultRegistry(PaymentSheetResult.Completed())

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<PaymentSheetResult>()
                val launcher = DefaultPaymentSheetLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.RESUMED)
                launcher.present(
                    mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"),
                    configuration = null,
                )
                assertThat(results).containsExactly(PaymentSheetResult.Completed())
            }
        }
    }

    @Test
    fun `init and present should fail when activity is not resumed`() {
        val testRegistry = FakeActivityResultRegistry(error = IllegalStateException("Invalid Activity State"))

        with(
            launchFragmentInContainer(initialState = Lifecycle.State.CREATED) {
                TestFragment()
            }
        ) {
            onFragment { fragment ->
                val results = mutableListOf<PaymentSheetResult>()
                val launcher = DefaultPaymentSheetLauncher(
                    fragment,
                    testRegistry
                ) {
                    results.add(it)
                }

                moveToState(Lifecycle.State.DESTROYED)
                launcher.present(
                    mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"),
                    configuration = null,
                )
                assertThat(results).hasSize(1)
                assertThat((results.first() as PaymentSheetResult.Failed).error).hasMessageThat().isEqualTo(
                    "The host activity is not in a valid state (INITIALIZED)."
                )
            }
        }
    }

    @Test
    fun `Clears out CreateIntentCallback when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[callbackIdentifier] = PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("I’m alive")
            }
            .build()

        val lifecycleOwner = TestLifecycleOwner()

        DefaultPaymentSheetLauncher(
            activityResultLauncher = mock(),
            activity = mock(),
            lifecycleOwner = lifecycleOwner,
            application = ApplicationProvider.getApplicationContext(),
            callback = mock(),
            id = callbackIdentifier,
        )

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(PaymentElementCallbackReferences[callbackIdentifier]).isNull()
    }

    @Test
    fun `Clears out externalPaymentMethodConfirmHandler when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[callbackIdentifier] = PaymentElementCallbacks.Builder()
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("I’m alive")
            }
            .build()

        val lifecycleOwner = TestLifecycleOwner()

        DefaultPaymentSheetLauncher(
            activityResultLauncher = mock(),
            activity = mock(),
            lifecycleOwner = lifecycleOwner,
            application = ApplicationProvider.getApplicationContext(),
            callback = mock(),
            id = callbackIdentifier,
        )

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.externalPaymentMethodConfirmHandler
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.externalPaymentMethodConfirmHandler
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.externalPaymentMethodConfirmHandler
        ).isNull()
    }

    @Test
    fun `Clears out confirmCustomPaymentMethodCallback when lifecycle owner is destroyed`() {
        PaymentElementCallbackReferences[callbackIdentifier] = PaymentElementCallbacks.Builder()
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("I’m alive")
            }
            .build()

        val lifecycleOwner = TestLifecycleOwner()

        DefaultPaymentSheetLauncher(
            activityResultLauncher = mock(),
            activity = mock(),
            lifecycleOwner = lifecycleOwner,
            application = ApplicationProvider.getApplicationContext(),
            callback = mock(),
            id = callbackIdentifier,
        )

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.confirmCustomPaymentMethodCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.confirmCustomPaymentMethodCallback
        ).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(
            PaymentElementCallbackReferences[callbackIdentifier]
                ?.confirmCustomPaymentMethodCallback
        ).isNull()
    }

    @Test
    fun `present uses the launcher ID for every presentation`() = runTest {
        val activityResultLauncher = FakeActivityResultLauncher<PaymentSheetContract.Args>()
        val id = Identifiable()

        launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }.use { scenario ->
            scenario.onFragment { fragment ->
                val sheet = PaymentSheet(
                    DefaultPaymentSheetLauncher(
                        activityResultLauncher = activityResultLauncher,
                        activity = fragment.requireActivity(),
                        lifecycleOwner = fragment,
                        application = fragment.requireActivity().application,
                        callback = { error("Result callback should not be called") },
                        id = id,
                        initializedViaCompose = false,
                    )
                )
                sheet.presentWithPaymentIntent("pi_first")
                sheet.presentWithSetupIntent("seti_first")
            }

            assertThat(activityResultLauncher.calls.awaitItem().input.paymentElementCallbackIdentifier).isEqualTo(id)
            assertThat(activityResultLauncher.calls.awaitItem().input.paymentElementCallbackIdentifier).isEqualTo(id)
            activityResultLauncher.calls.expectNoEvents()
            activityResultLauncher.unregisterCalls.expectNoEvents()
        }
    }

    private class FakeActivityResultRegistry(
        private val result: PaymentSheetResult? = null,
        private val error: Throwable? = null
    ) : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            if (error != null) {
                throw error
            }
            dispatchResult(
                requestCode,
                result
            )
        }
    }

    internal class TestFragment : Fragment()
}
