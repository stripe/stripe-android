package com.stripe.android.payments.core.authentication.threeds2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.R
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.views.ChallengeProgressFragmentFactory
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2TransactionActivityTest {

    @After
    fun cleanUpInjector() {
        WeakMapInjectorRegistry.clear()
    }

    @Test
    fun `start without args should finish with Error result`() {
        ActivityScenario.launchActivityForResult(
            Stripe3ds2TransactionActivity::class.java,
            Bundle()
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result = parseResult(activityScenario)
            assertThat(result.exception?.message)
                .isEqualTo(
                    "Error while attempting to initiate 3DS2 transaction."
                )
        }
    }

    @Test
    fun `fragmentFactory should be a ChallengeProgressFragmentFactory`() {
        val viewModel = mock<Stripe3ds2TransactionViewModel>()
        viewModel.stub {
            on { viewModel.start3ds2Flow() } doReturn NextStep.Complete(PaymentFlowResult.Unvalidated())
        }
        injectableActivityScenario<Stripe3ds2TransactionActivity> {
            injectActivity {
                viewModelFactory =
                    TestUtils.viewModelFactoryFor(viewModel)
            }
        }.launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                Stripe3ds2TransactionActivity::class.java
            ).putExtras(
                ARGS.toBundle()
            )
        ).use { activityScenario ->
            activityScenario.onActivity { activity ->
                assertIs<ChallengeProgressFragmentFactory>(
                    activity.supportFragmentManager.fragmentFactory
                )
            }
        }
    }

    @Test
    fun `navigation bar inset is applied below content without shifting it sideways`() {
        runInsetsScenario(bottomInset = 78)
    }

    @Test
    fun `zero navigation bar inset preserves horizontal padding`() {
        runInsetsScenario(bottomInset = 0)
    }

    private fun runInsetsScenario(bottomInset: Int) {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            Stripe3ds2TransactionActivity::class.java
        ).putExtras(ARGS.toBundle())

        Robolectric.buildActivity(Stripe3ds2TransactionActivity::class.java, intent).use { controller ->
            // Do not resume: authentication must not start for this layout test.
            val activity = controller.create().get()
            val root = activity.findViewById<View>(R.id.fragment_container)
            root.layoutDirection = View.LAYOUT_DIRECTION_LTR
            root.setPaddingRelative(11, 13, 17, 19)
            val insets = WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(0, 24, 0, bottomInset)
                )
                .build()

            repeat(2) {
                val remainingInsets = ViewCompat.dispatchApplyWindowInsets(root, insets)

                assertThat(root.paddingStart).isEqualTo(11)
                assertThat(root.paddingTop).isEqualTo(13)
                assertThat(root.paddingEnd).isEqualTo(17)
                assertThat(root.paddingBottom).isEqualTo(bottomInset)
                // FragmentContainerView returns the original insets to its parent.
                assertThat(remainingInsets.getInsets(WindowInsetsCompat.Type.systemBars()))
                    .isEqualTo(insets.getInsets(WindowInsetsCompat.Type.systemBars()))
            }
        }
    }

    private fun parseResult(
        activityScenario: ActivityScenario<*>
    ): PaymentFlowResult.Unvalidated {
        return Stripe3ds2TransactionContract()
            .parseResult(0, activityScenario.result.resultData)
    }

    private companion object {
        val ARGS = Stripe3ds2TransactionContract.Args(
            SdkTransactionId.create(),
            PaymentAuthConfig.Stripe3ds2Config(
                timeout = 5,
                PaymentAuthConfig.Stripe3ds2UiCustomization(
                    StripeUiCustomization()
                )
            ),
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2,
            PaymentIntentFixtures.PI_REQUIRES_MASTERCARD_3DS2.nextActionData
                as StripeIntent.NextActionData.SdkData.Use3DS2,
            ApiRequest.Options(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY),
            enableLogging = false,
            statusBarColor = null,
            ApiConfiguration.State(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "acct_123"),
            setOf()
        )
    }
}
