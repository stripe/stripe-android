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
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetLauncherTest {
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
                launcher.present(mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"))
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
                launcher.present(mode = PaymentElementLoader.InitializationMode.PaymentIntent("pi_fake"))
                assertThat(results).hasSize(1)
                assertThat((results.first() as PaymentSheetResult.Failed).error).hasMessageThat().isEqualTo(
                    "The host activity is not in a valid state (INITIALIZED)."
                )
            }
        }
    }

    @Test
    fun `Destroying a launcher does not remove callbacks registered by another owner`() {
        val callbackOwner = TestLifecycleOwner()
        val callbacks = PaymentElementCallbacks.Builder().build()
        PaymentElementCallbackReferences.register(PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER, callbackOwner, callbacks)

        launchFragmentInContainer(initialState = Lifecycle.State.CREATED) { TestFragment() }.use { scenario ->
            scenario.onFragment { fragment ->
                DefaultPaymentSheetLauncher(fragment, FakeActivityResultRegistry(PaymentSheetResult.Completed())) {
                    error("Should not be called!")
                }
            }

            scenario.moveToState(Lifecycle.State.DESTROYED)

            assertThat(PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER])
                .isSameInstanceAs(callbacks)
        }
        callbackOwner.currentState = Lifecycle.State.DESTROYED
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
