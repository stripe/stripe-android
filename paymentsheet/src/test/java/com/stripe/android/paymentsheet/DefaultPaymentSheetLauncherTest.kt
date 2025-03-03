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
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetLauncherTest {

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `init and present should return expected PaymentResult`() {
        val testRegistry = FakeActivityResultRegistry(PaymentSheetResult.Completed)

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
                launcher.present(mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"))
                assertThat(results).containsExactly(PaymentSheetResult.Completed)
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
                launcher.present(mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"))
                assertThat(results).hasSize(1)
                assertThat((results.first() as PaymentSheetResult.Failed).error).hasMessageThat().isEqualTo(
                    "The host activity is not in a valid state (INITIALIZED)."
                )
            }
        }
    }

    @Test
    fun `Clears out CreateIntentCallback when lifecycle owner is destroyed`() {
        val paymentSheetKey = "PaymentSheet"

        PaymentElementCallbackReferences[paymentSheetKey] = PaymentElementCallbacks(
            createIntentCallback = { _, _ ->
                error("I’m alive")
            },
            externalPaymentMethodConfirmHandler = null,
        )

        val lifecycleOwner = TestLifecycleOwner()

        DefaultPaymentSheetLauncher(
            activityResultLauncher = mock(),
            activity = mock(),
            lifecycleOwner = lifecycleOwner,
            application = ApplicationProvider.getApplicationContext(),
            callback = mock(),
        )

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]).isNull()
    }

    @Test
    fun `Clears out externalPaymentMethodConfirmHandler when lifecycle owner is destroyed`() {
        val paymentSheetKey = "PaymentSheet"

        PaymentElementCallbackReferences[paymentSheetKey] = PaymentElementCallbacks(
            createIntentCallback = null,
            externalPaymentMethodConfirmHandler = { _, _ ->
                error("I’m alive")
            },
        )

        val lifecycleOwner = TestLifecycleOwner()

        DefaultPaymentSheetLauncher(
            activityResultLauncher = mock(),
            activity = mock(),
            lifecycleOwner = lifecycleOwner,
            application = ApplicationProvider.getApplicationContext(),
            callback = mock(),
        )

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]?.externalPaymentMethodConfirmHandler).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]?.externalPaymentMethodConfirmHandler).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(PaymentElementCallbackReferences[paymentSheetKey]?.externalPaymentMethodConfirmHandler).isNull()
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
