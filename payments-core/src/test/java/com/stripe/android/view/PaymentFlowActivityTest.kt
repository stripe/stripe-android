package com.stripe.android.view

import android.app.Activity
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.R
import com.stripe.android.model.ShippingMethod
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentFlowActivityTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val ephemeralKeyProvider: EphemeralKeyProvider = mock()
    private val shippingInformationValidator: PaymentSessionConfig.ShippingInformationValidator = mock()
    private val shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory = mock()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
        CustomerSession.initCustomerSession(context, ephemeralKeyProvider)
    }

    @Test
    fun launchPaymentFlowActivity_withInvalidArgs_finishesActivity() {
        activityScenarioFactory.create<PaymentFlowActivity>().use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            createStarterArgs(
                PaymentSessionFixtures.CONFIG.copy(
                    isShippingInfoRequired = false
                )
            )
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertThat(getShippingInfoWidget(paymentFlowActivity))
                    .isNull()
                assertThat(getShippingMethodWidget(paymentFlowActivity))
                    .isNotNull()
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertThat(getShippingInfoWidget(paymentFlowActivity))
                    .isNotNull()
                paymentFlowActivity.onActionSave()
                assertThat(paymentFlowActivity.isFinishing)
                    .isFalse()
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertThat(getShippingInfoWidget(paymentFlowActivity))
                    .isNotNull()
                paymentFlowActivity.onActionSave()
                assertThat(paymentFlowActivity.isFinishing)
                    .isFalse()
                assertThat(getShippingInfoWidget(paymentFlowActivity))
                    .isNotNull()
            }
        }
    }

    @Test
    fun onShippingInfoProcessed_whenValidShippingInfoSubmitted_rendersCorrectly() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            createStarterArgs(
                PaymentSessionFixtures.CONFIG.copy(
                    prepopulatedShippingInfo = SHIPPING_INFO
                )
            )
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // valid result
                paymentFlowActivity.onActionSave()

                assertThat(paymentFlowActivity.progressBar.isVisible)
                    .isTrue()
                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO, SHIPPING_METHODS)
                assertThat(paymentFlowActivity.progressBar.isGone)
                    .isTrue()
            }
        }
    }

    @Test
    fun onShippingInfoSaved_whenOnlyShippingInfo_finishWithSuccess() {
        activityScenarioFactory.createForResult<PaymentFlowActivity>(
            createStarterArgs(
                PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .setShippingMethodsRequired(false)
                    .build()
            )
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // valid result
                paymentFlowActivity.onActionSave()

                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO)
                assertThat(paymentFlowActivity.isFinishing)
                    .isTrue()

                assertThat(activityScenario.result.resultCode)
                    .isEqualTo(Activity.RESULT_OK)
                val resultData = activityScenario.result.resultData

                val resultSessionData: PaymentSessionData? =
                    resultData.extras?.getParcelable(EXTRA_PAYMENT_SESSION_DATA)
                assertThat(resultSessionData?.shippingInformation)
                    .isEqualTo(SHIPPING_INFO)
            }
        }
    }

    @Test
    fun onShippingInfoSaved_withValidatorAndFactory() {
        `when`(shippingInformationValidator.isValid(SHIPPING_INFO)).thenReturn(true)
        `when`(shippingMethodsFactory.create(SHIPPING_INFO)).thenReturn(SHIPPING_METHODS)

        activityScenarioFactory.create<PaymentFlowActivity>(
            createStarterArgs(
                PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .setShippingInformationValidator(shippingInformationValidator)
                    .setShippingMethodsFactory(shippingMethodsFactory)
                    .build()
            )
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // valid result
                paymentFlowActivity.onActionSave()
                idleLooper()

                assertThat(paymentFlowActivity.progressBar.isVisible)
                    .isTrue()
                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO, SHIPPING_METHODS)
                idleLooper()
                assertThat(paymentFlowActivity.progressBar.isGone)
                    .isTrue()

                verify(shippingInformationValidator).isValid(SHIPPING_INFO)
                verify(shippingInformationValidator, never()).getErrorMessage(SHIPPING_INFO)
                verify(shippingMethodsFactory).create(SHIPPING_INFO)
            }
        }
    }

    private fun getShippingInfoWidget(activity: Activity): ShippingInfoWidget? {
        return activity.findViewById(R.id.shipping_info_widget)
    }

    private fun getShippingMethodWidget(activity: Activity): SelectShippingMethodWidget? {
        return activity.findViewById(R.id.select_shipping_method_widget)
    }

    private companion object {
        private val SHIPPING_INFO = ShippingInfoFixtures.DEFAULT
        private val SHIPPING_METHODS = arrayListOf(
            ShippingMethod(
                "UPS Ground",
                "ups-ground",
                0,
                "USD",
                "Arrives in 3-5 days"
            ),
            ShippingMethod(
                "FedEx",
                "fedex",
                599,
                "USD",
                "Arrives tomorrow"
            )
        )

        private fun createStarterArgs(
            config: PaymentSessionConfig
        ): PaymentFlowActivityStarter.Args {
            return PaymentFlowActivityStarter.Args(
                paymentSessionConfig = config,
                paymentSessionData = PaymentSessionData(config)
            )
        }
    }
}
