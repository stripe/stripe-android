package com.stripe.android.view

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentFlowActivityTest {

    private val ephemeralKeyProvider: EphemeralKeyProvider = mock()
    private val shippingInformationValidator: PaymentSessionConfig.ShippingInformationValidator = mock()
    private val shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory = mock()

    private val context: Context by lazy {
        ApplicationProvider.getApplicationContext<Context>()
    }
    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        CustomerSession.initCustomerSession(context, ephemeralKeyProvider)
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
                assertNull(getShippingInfoWidget(paymentFlowActivity))
                assertNotNull(getShippingMethodWidget(paymentFlowActivity))
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertNotNull(getShippingInfoWidget(paymentFlowActivity))
                paymentFlowActivity.onActionSave()
                assertFalse(paymentFlowActivity.isFinishing)
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertNotNull(getShippingInfoWidget(paymentFlowActivity))
                paymentFlowActivity.onActionSave()
                assertFalse(paymentFlowActivity.isFinishing)
                assertNotNull(getShippingInfoWidget(paymentFlowActivity))
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

                assertEquals(View.VISIBLE, paymentFlowActivity.progressBar.visibility)

                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO, SHIPPING_METHODS)
                assertEquals(View.GONE, paymentFlowActivity.progressBar.visibility)
            }
        }
    }

    @Test
    fun onShippingInfoSaved_whenOnlyShippingInfo_finishWithSuccess() {
        activityScenarioFactory.create<PaymentFlowActivity>(
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
                assertTrue(paymentFlowActivity.isFinishing)

                assertEquals(activityScenario.result.resultCode, Activity.RESULT_OK)
                val resultData = activityScenario.result.resultData

                val resultSessionData: PaymentSessionData? =
                    resultData.extras?.getParcelable(EXTRA_PAYMENT_SESSION_DATA)
                assertEquals(resultSessionData?.shippingInformation, SHIPPING_INFO)
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
                assertEquals(View.VISIBLE, paymentFlowActivity.progressBar.visibility)
                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO, SHIPPING_METHODS)
                assertEquals(View.GONE, paymentFlowActivity.progressBar.visibility)

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
            ShippingMethod("UPS Ground", "ups-ground",
                0, "USD", "Arrives in 3-5 days"),
            ShippingMethod("FedEx", "fedex",
                599, "USD", "Arrives tomorrow")
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
