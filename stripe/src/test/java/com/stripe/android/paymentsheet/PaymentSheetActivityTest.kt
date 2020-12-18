package com.stripe.android.paymentsheet

import android.content.Intent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.StripePaymentController
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.InjectableActivityScenario
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.utils.TestUtils.viewModelFactoryFor
import com.stripe.android.utils.injectableActivityScenario
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class PaymentSheetActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    private val paymentMethods = listOf(
        PaymentMethod("payment_method_id", 0, false, PaymentMethod.Type.Card)
    )

    private val googlePayRepository = FakeGooglePayRepository(true)
    private val stripeRepository = FakeStripeRepository(PAYMENT_INTENT, paymentMethods)
    private val eventReporter = mock<EventReporter>()

    private val viewModel = PaymentSheetViewModel(
        publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        stripeAccountId = null,
        stripeRepository = stripeRepository,
        paymentController = StripePaymentController(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeRepository,
            workContext = testCoroutineDispatcher
        ),
        googlePayRepository = googlePayRepository,
        prefsRepository = mock(),
        eventReporter = eventReporter,
        args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
        workContext = testCoroutineDispatcher
    )

    private val intent = Intent(
        ApplicationProvider.getApplicationContext(),
        PaymentSheetActivity::class.java
    ).putExtra(
        ActivityStarter.Args.EXTRA,
        PaymentSheetContract.Args(
            "client_secret",
            PaymentSheetFixtures.CONFIG_CUSTOMER
        )
    )

    @BeforeTest
    fun before() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handles clicks outside of bottom sheet`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_COLLAPSED)

            activity.viewBinding.root.performClick()
            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            assertThat(
                PaymentSheet.Result.fromIntent(shadowOf(activity).resultIntent)
            ).isEqualTo(
                PaymentSheet.Result(
                    PaymentResult.Cancelled(
                        null,
                        PAYMENT_INTENT
                    )
                )
            )
        }
    }

    @Test
    fun `updates buy button state`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()

            viewModel.updateSelection(PaymentSelection.GooglePay)
            assertThat(activity.viewBinding.buyButton.isEnabled).isTrue()

            viewModel.updateSelection(null)
            assertThat(activity.viewBinding.buyButton.isEnabled).isFalse()
        }
    }

    @Test
    fun `handles fragment transitions`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetPaymentMethodsListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_COLLAPSED)
            assertThat(activity.viewBinding.bottomSheet.layoutParams.height)
                .isEqualTo(WRAP_CONTENT)

            viewModel.transitionTo(PaymentSheetViewModel.TransitionTarget.AddPaymentMethodFull)
            idleLooper()
            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetAddCardFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_EXPANDED)
            assertThat(activity.viewBinding.bottomSheet.layoutParams.height)
                .isEqualTo(MATCH_PARENT)

            activity.onBackPressed()
            idleLooper()
            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetPaymentMethodsListFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_COLLAPSED)
            assertThat(activity.viewBinding.bottomSheet.layoutParams.height)
                .isEqualTo(WRAP_CONTENT)

            activity.onBackPressed()
            idleLooper()
            // animating out
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            assertThat(
                PaymentSheet.Result.fromIntent(shadowOf(activity).resultIntent)
            ).isEqualTo(
                PaymentSheet.Result(
                    PaymentResult.Cancelled(
                        null,
                        PAYMENT_INTENT
                    )
                )
            )
        }
    }

    @Test
    fun `handles buy button clicks`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            viewModel.updateSelection(
                PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()

            activity.viewBinding.buyButton.performClick()
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()

            // payment intent was confirmed and result will be communicated through PaymentRelayActivity
            val nextActivity = shadowOf(activity).peekNextStartedActivity()
            assertThat(nextActivity.component?.className)
                .isEqualTo(PaymentRelayActivity::class.java.name)
        }
    }

    @Test
    fun `reports successful payment intent result`() {
        val scenario = activityScenario()
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(500)
            idleLooper()

            viewModel.onActivityResult(
                StripePaymentController.PAYMENT_REQUEST_CODE, 0,
                Intent().apply {
                    putExtras(
                        PaymentController.Result(
                            "client_secret",
                            StripeIntentResult.Outcome.SUCCEEDED
                        ).toBundle()
                    )
                }
            )
            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            assertThat(PaymentSheet.Result.fromIntent(shadowOf(activity).resultIntent))
                .isEqualTo(
                    PaymentSheet.Result(
                        PaymentResult.Succeeded(PAYMENT_INTENT)
                    )
                )
        }
    }

    @Test
    fun `shows add card fragment when no payment methods available`() {
        val viewModel = PaymentSheetViewModel(
            publishableKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            stripeAccountId = null,
            stripeRepository = FakeStripeRepository(PAYMENT_INTENT, listOf()),
            paymentController = StripePaymentController(
                ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                stripeRepository,
                workContext = testCoroutineDispatcher
            ),
            googlePayRepository = googlePayRepository,
            prefsRepository = mock(),
            eventReporter = eventReporter,
            args = PaymentSheetFixtures.ARGS_CUSTOMER_WITH_GOOGLEPAY,
            workContext = testCoroutineDispatcher
        )

        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            assertThat(currentFragment(activity))
                .isInstanceOf(PaymentSheetAddCardFragment::class.java)
            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_COLLAPSED)
            assertThat(activity.viewBinding.bottomSheet.layoutParams.height)
                .isEqualTo(MATCH_PARENT)

            // make sure loading fragment isn't in back stack
            activity.onBackPressed()
            idleLooper()

            assertThat(activity.bottomSheetBehavior.state)
                .isEqualTo(BottomSheetBehavior.STATE_HIDDEN)
            assertThat(
                PaymentSheet.Result.fromIntent(shadowOf(activity).resultIntent)
            ).isEqualTo(
                PaymentSheet.Result(
                    PaymentResult.Cancelled(
                        null,
                        PAYMENT_INTENT
                    )
                )
            )
        }
    }

    @Test
    fun `buyButton is only enabled when not processing, transition target, and a selection has been made`() {
        val scenario = activityScenario(viewModel)
        scenario.launch(intent).onActivity { activity ->
            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()
            // wait for bottom sheet to animate in
            testCoroutineDispatcher.advanceTimeBy(BottomSheetController.ANIMATE_IN_DELAY)
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isFalse()

            viewModel.updateSelection(PaymentSelection.GooglePay)
            idleLooper()

            assertThat(activity.viewBinding.buyButton.isEnabled)
                .isTrue()
        }
    }

    private fun currentFragment(activity: PaymentSheetActivity) =
        activity.supportFragmentManager.findFragmentById(activity.viewBinding.fragmentContainer.id)

    private fun activityScenario(
        viewModel: PaymentSheetViewModel = this.viewModel
    ): InjectableActivityScenario<PaymentSheetActivity> {
        return injectableActivityScenario {
            injectActivity {
                viewModelFactory = viewModelFactoryFor(viewModel)
            }
        }
    }

    private class FakeStripeRepository(
        val paymentIntent: PaymentIntent,
        val paymentMethods: List<PaymentMethod>
    ) : AbsFakeStripeRepository() {
        override suspend fun getPaymentMethods(
            listPaymentMethodsParams: ListPaymentMethodsParams,
            publishableKey: String,
            productUsageTokens: Set<String>,
            requestOptions: ApiRequest.Options
        ): List<PaymentMethod> {
            return paymentMethods
        }

        override suspend fun confirmPaymentIntent(
            confirmPaymentIntentParams: ConfirmPaymentIntentParams,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent = paymentIntent

        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ): PaymentIntent = paymentIntent
    }

    private companion object {
        private val PAYMENT_INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    }
}
