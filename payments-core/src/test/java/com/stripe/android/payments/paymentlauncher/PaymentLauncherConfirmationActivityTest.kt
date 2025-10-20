package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import android.graphics.Color
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.R
import com.stripe.android.core.exception.GenericStripeException
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils
import com.stripe.android.utils.injectableActivityScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentLauncherConfirmationActivityTest {
    private val viewModel = mock<PaymentLauncherViewModel> {
        on { it.internalPaymentResult } doReturn MutableStateFlow(null)
    }
    private val testFactory = TestUtils.viewModelFactoryFor(viewModel)

    @Test
    fun `Ensure title is 'Confirming transaction'`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    confirmStripeIntentParams = mock(),
                    statusBarColor = Color.CYAN
                ).toBundle()
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.title).isEqualTo(
                    activity.resources.getString(R.string.stripe_confirming_transaction_status)
                )
            }
        }
    }

    @Test
    fun `statusBarColor is set to transparent on window`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    confirmStripeIntentParams = mock(),
                    statusBarColor = Color.CYAN
                ).toBundle()
            )
        ).use {
            it.onActivity { activity ->
                assertThat(activity.window.statusBarColor).isEqualTo(Color.TRANSPARENT)
            }
        }
    }

    @Test
    fun `start should call register`() {
        val confirmStripeIntentParams = mock<ConfirmStripeIntentParams>()
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    confirmStripeIntentParams = confirmStripeIntentParams,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).register(any(), any())
                }
            }
        }
    }

    @Test
    fun `start with IntentConfirmationArgs should confirmStripeIntent`() {
        val confirmStripeIntentParams = mock<ConfirmStripeIntentParams>()
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.IntentConfirmationArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    confirmStripeIntentParams = confirmStripeIntentParams,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).confirmStripeIntent(eq(confirmStripeIntentParams), any())
                }
            }
        }
    }

    @Test
    fun `start with PaymentIntentNextActionArgs should handleNextActionForStripeIntent`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PAYMENT_INTENT_NEXT_ACTION_ARGS.toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).handleNextActionForStripeIntent(eq(CLIENT_SECRET), any())
                }
            }
        }
    }

    @Test
    fun `start with SetupIntentNextActionArgs should handleNextActionForStripeIntent`() {
        mockViewModelActivityScenario().launch(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(
                PaymentLauncherContract.Args.SetupIntentNextActionArgs(
                    publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                    stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                    enableLogging = false,
                    productUsage = PRODUCT_USAGE,
                    includePaymentSheetNextHandlers = false,
                    setupIntentClientSecret = CLIENT_SECRET,
                    statusBarColor = Color.RED,
                ).toBundle()
            )
        ).use {
            it.onActivity {
                runTest {
                    verify(viewModel).handleNextActionForStripeIntent(eq(CLIENT_SECRET), any())
                    verify(viewModel).register(any(), any())
                }
            }
        }
    }

    @Test
    fun `start without args should finish with Error result`() {
        mockViewModelActivityScenario().launchForResult(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            )
        ).use { activityScenario ->
            assertThat(activityScenario.state)
                .isEqualTo(Lifecycle.State.DESTROYED)
            val result =
                InternalPaymentResult.fromIntent(
                    activityScenario.getResult().resultData
                ) as InternalPaymentResult.Failed
            assertThat(result.throwable.message)
                .isEqualTo(
                    PaymentLauncherConfirmationActivity.EMPTY_ARG_ERROR
                )
        }
    }

    @Test
    fun `start with non-Base64 hashed value should fail`() = testHashedValueFailure(
        hashedValue = "random_value===",
        analyticsValue = "invalidHashedValueNotBase64",
    )

    @Test
    fun `start with invalid formatted hashed value should fail`() = testHashedValueFailure(
        hashedValue = "random_value",
        analyticsValue = "invalidHashedValueIncorrectFormat",
    )

    @Test
    fun `start with valid formatted hashed value should call expected function`() {
        val args = PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs(
            hashedValue = Base64.encodeToString("$PUBLISHABLE_KEY:$CLIENT_SECRET".toByteArray(), 0),
            stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
            enableLogging = false,
            productUsage = PRODUCT_USAGE,
            includePaymentSheetNextHandlers = false,
            statusBarColor = Color.RED,
        )

        mockViewModelActivityScenario().launchForResult(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(args.toBundle())
        ).use { activityScenario ->
            assertThat(args.publishableKey).isEqualTo(PUBLISHABLE_KEY)
            assertThat(args.paymentIntentClientSecret).isEqualTo(CLIENT_SECRET)

            activityScenario.onActivity {
                runTest {
                    verify(viewModel).handleNextActionForStripeIntent(eq(CLIENT_SECRET), any())
                    verify(viewModel).register(any(), any())
                }
            }
        }
    }

    private fun testHashedValueFailure(
        hashedValue: String,
        analyticsValue: String,
    ) {
        val args = PaymentLauncherContract.Args.HashedPaymentIntentNextActionArgs(
            hashedValue = hashedValue,
            stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
            enableLogging = false,
            productUsage = PRODUCT_USAGE,
            includePaymentSheetNextHandlers = false,
            statusBarColor = Color.RED,
        )

        mockViewModelActivityScenario().launchForResult(
            Intent(
                ApplicationProvider.getApplicationContext(),
                PaymentLauncherConfirmationActivity::class.java
            ).putExtras(args.toBundle())
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)

            val result = InternalPaymentResult.fromIntent(activityScenario.getResult().resultData)

            assertThat(result).isInstanceOf(InternalPaymentResult.Failed::class.java)

            val failedResult = result as InternalPaymentResult.Failed
            val throwable = failedResult.throwable

            assertThat(throwable).isInstanceOf(GenericStripeException::class.java)

            val stripeException = throwable as GenericStripeException

            assertThat(stripeException.analyticsValue()).isEqualTo(analyticsValue)
            assertThat(stripeException.message).isEqualTo(
                "Invalid hashed value! Please provided a hashed payment intent in the correct format!"
            )

            assertThat(args.paymentIntentClientSecret).isEqualTo("UNKNOWN")
            assertThat(args.publishableKey).isEqualTo("UNKNOWN")
        }
    }

    private fun mockViewModelActivityScenario(): InjectableActivityScenario<PaymentLauncherConfirmationActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = testFactory
            }
        }
    }

    private companion object {
        const val PUBLISHABLE_KEY = "publishableKey"
        const val CLIENT_SECRET = "clientSecret"
        const val TEST_STRIPE_ACCOUNT_ID = "accountId"
        val PRODUCT_USAGE = setOf("TestProductUsage")
        val PAYMENT_INTENT_NEXT_ACTION_ARGS =
            PaymentLauncherContract.Args.PaymentIntentNextActionArgs(
                publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeAccountId = TEST_STRIPE_ACCOUNT_ID,
                enableLogging = false,
                productUsage = PRODUCT_USAGE,
                includePaymentSheetNextHandlers = false,
                paymentIntentClientSecret = CLIENT_SECRET,
                statusBarColor = Color.RED,
            )
    }
}
