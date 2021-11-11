package com.stripe.android.payments.core.authentication.threeds2

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.ApiRequest
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.views.ChallengeProgressFragmentFactory
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import org.junit.After
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2TransactionActivityTest {

    @After
    fun cleanUpInjector() {
        WeakMapInjectorRegistry.staticCacheMap.clear()
    }

    @Test
    fun `start without args should finish with Error result`() {
        ActivityScenario.launch(
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
        injectableActivityScenario<Stripe3ds2TransactionActivity> {
            injectActivity {
                viewModelFactory =
                    TestUtils.viewModelFactoryFor(mock<Stripe3ds2TransactionViewModel>())
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
            injectorKey = DUMMY_INJECTOR_KEY,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            setOf()
        )
    }
}
