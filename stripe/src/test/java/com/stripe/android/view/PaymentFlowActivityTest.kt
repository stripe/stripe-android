package com.stripe.android.view

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.CustomerSession
import com.stripe.android.CustomerSession.Companion.ACTION_API_EXCEPTION
import com.stripe.android.CustomerSession.Companion.EXTRA_EXCEPTION
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession.Companion.EXTRA_PAYMENT_SESSION_DATA
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.PaymentSessionFixtures
import com.stripe.android.R
import com.stripe.android.exception.APIException
import com.stripe.android.model.ShippingMethod
import com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED
import com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID
import com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA
import com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentFlowActivityTest {

    private var shippingInfoWidget: ShippingInfoWidget? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager

    @Mock
    private lateinit var ephemeralKeyProvider: EphemeralKeyProvider
    @Mock
    private lateinit var broadcastReceiver: BroadcastReceiver
    @Mock
    private lateinit var shippingInformationValidator: PaymentSessionConfig.ShippingInformationValidator
    @Mock
    private lateinit var shippingMethodsFactory: PaymentSessionConfig.ShippingMethodsFactory

    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>

    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
        MockitoAnnotations.initMocks(this)
        intentArgumentCaptor = argumentCaptor()

        val context: Context = ApplicationProvider.getApplicationContext()
        localBroadcastManager = LocalBroadcastManager.getInstance(context)
        localBroadcastManager.registerReceiver(broadcastReceiver,
            IntentFilter(PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED))
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(),
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        CustomerSession.initCustomerSession(context, ephemeralKeyProvider)
    }

    @AfterTest
    fun tearDown() {
        localBroadcastManager.unregisterReceiver(broadcastReceiver)
    }

    @Test
    fun launchPaymentFlowActivity_withHideShippingInfoConfig_hidesShippingInfoView() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setShippingInfoRequired(false)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                assertNull(paymentFlowActivity.findViewById(R.id.shipping_info_widget))
                assertNotNull(
                    paymentFlowActivity.findViewById<View>(R.id.select_shipping_method_widget)
                )
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingNotPopulated_doesNotFinish() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                shippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget)
                assertNotNull(shippingInfoWidget)
                paymentFlowActivity.onActionSave()
                assertFalse(paymentFlowActivity.isFinishing)
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingInfoNotPopulated_doesNotContinue() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                shippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget)
                assertNotNull(shippingInfoWidget)
                paymentFlowActivity.onActionSave()
                assertFalse(paymentFlowActivity.isFinishing)
                assertNotNull(paymentFlowActivity.findViewById<View>(R.id.shipping_info_widget))
            }
        }
    }

    @Test
    fun onShippingInfoSave_whenShippingPopulated_sendsCorrectIntent() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                shippingInfoWidget = paymentFlowActivity.findViewById(R.id.shipping_info_widget)
                assertNotNull(shippingInfoWidget)
                paymentFlowActivity.onActionSave()
                verify(broadcastReceiver).onReceive(any(), intentArgumentCaptor.capture())

                assertEquals(
                    requireNotNull(
                        intentArgumentCaptor.firstValue.getParcelableExtra(EXTRA_SHIPPING_INFO_DATA)
                    ),
                    SHIPPING_INFO
                )
            }
        }
    }

    @Test
    fun onErrorBroadcast_displaysAlertDialog() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                val listener =
                    mock(StripeActivity.AlertMessageListener::class.java)
                paymentFlowActivity.setAlertMessageListener(listener)

                val bundle = Bundle()
                bundle.putSerializable(EXTRA_EXCEPTION,
                    APIException("Something's wrong", "ID123", 400, null, null))

                localBroadcastManager.sendBroadcast(
                    Intent(ACTION_API_EXCEPTION)
                        .putExtras(bundle)
                )

                verify(listener).onAlertMessageDisplayed("Something's wrong")
            }
        }
    }

    @Test
    fun onShippingInfoProcessed_whenInvalidShippingInfoSubmitted_rendersCorrectly() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // invalid result
                paymentFlowActivity.onActionSave()
                assertEquals(paymentFlowActivity.progressBar.visibility, View.VISIBLE)

                localBroadcastManager.sendBroadcast(
                    Intent(EVENT_SHIPPING_INFO_PROCESSED)
                        .putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false)
                )
                assertEquals(paymentFlowActivity.progressBar.visibility, View.GONE)
            }
        }
    }

    @Test
    fun onShippingInfoProcessed_whenValidShippingInfoSubmitted_rendersCorrectly() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // valid result
                paymentFlowActivity.onActionSave()

                localBroadcastManager.sendBroadcast(
                    Intent(EVENT_SHIPPING_INFO_PROCESSED)
                        .putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true)
                        .putExtra(EXTRA_VALID_SHIPPING_METHODS, SHIPPING_METHODS)
                )
                assertEquals(View.VISIBLE, paymentFlowActivity.progressBar.visibility)

                paymentFlowActivity.onShippingInfoSaved(SHIPPING_INFO, SHIPPING_METHODS)
                assertEquals(View.GONE, paymentFlowActivity.progressBar.visibility)
            }
        }
    }

    @Test
    fun onShippingInfoSaved_whenOnlyShippingInfo_finishWithSuccess() {
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .setShippingMethodsRequired(false)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
        ).use { activityScenario ->
            activityScenario.onActivity { paymentFlowActivity ->
                // valid result
                paymentFlowActivity.onActionSave()

                localBroadcastManager.sendBroadcast(
                    Intent(EVENT_SHIPPING_INFO_PROCESSED)
                        .putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true)
                )

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
    fun onShippingInfoSaved_withValidatorAndFactory_doesNotSendBroadcast() {
        verifyNoMoreInteractions(broadcastReceiver)

        `when`(shippingInformationValidator.isValid(SHIPPING_INFO)).thenReturn(true)
        `when`(shippingMethodsFactory.create(SHIPPING_INFO)).thenReturn(SHIPPING_METHODS)

        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                    .setPrepopulatedShippingInfo(SHIPPING_INFO)
                    .setShippingInformationValidator(shippingInformationValidator)
                    .setShippingMethodsFactory(shippingMethodsFactory)
                    .build())
                .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
                .build()
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

    private companion object {
        private val SHIPPING_INFO = ShippingInfoFixtures.DEFAULT
        private val SHIPPING_METHODS = arrayListOf(
            ShippingMethod("UPS Ground", "ups-ground",
                0, "USD", "Arrives in 3-5 days"),
            ShippingMethod("FedEx", "fedex",
                599, "USD", "Arrives tomorrow")
        )
    }
}
