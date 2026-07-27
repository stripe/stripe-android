package com.stripe.android.crypto.onramp.samsungpay

import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.analytics.OnrampAnalyticsEvent
import com.stripe.android.crypto.onramp.model.OnrampConfiguration
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
class DefaultSamsungPayLauncherTest {

    @Test
    fun `ready status is reported`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
    }

    @Test
    fun `not supported status is reported`() = runScenario(
        statusResult = FakeSamsungPaySdkState.StatusResult.Success(
            status = SpaySdk.SPAY_NOT_SUPPORTED,
            data = Bundle(),
        ),
    ) {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.NotSupported)
    }

    @Test
    fun `temporarily unavailable status is reported`() = runScenario(
        statusResult = FakeSamsungPaySdkState.StatusResult.Success(
            status = SpaySdk.SPAY_NOT_ALLOWED_TEMPORALLY,
            data = Bundle(),
        ),
    ) {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.TemporarilyUnavailable)
    }

    @Test
    fun `not ready setup status includes setup reason`() = runScenario(
        statusResult = notReadyStatus(SpaySdk.ERROR_SPAY_SETUP_NOT_COMPLETED),
    ) {
        assertThat(getStatus()).isEqualTo(
            SamsungPayStatus.NotReady(SamsungPayStatus.NotReady.Reason.NeedsUserSetup),
        )
    }

    @Test
    fun `not ready update status includes update reason`() = runScenario(
        statusResult = notReadyStatus(SpaySdk.ERROR_SPAY_APP_NEED_TO_UPDATE),
    ) {
        assertThat(getStatus()).isEqualTo(
            SamsungPayStatus.NotReady(SamsungPayStatus.NotReady.Reason.NeedsAppUpdate),
        )
    }

    @Test
    fun `not ready unknown status preserves reason code`() = runScenario(
        statusResult = notReadyStatus(UNKNOWN_NOT_READY_REASON),
    ) {
        assertThat(getStatus()).isEqualTo(
            SamsungPayStatus.NotReady(SamsungPayStatus.NotReady.Reason.Other(UNKNOWN_NOT_READY_REASON)),
        )
    }

    @Test
    fun `status callback failure is reported`() = runScenario(
        statusResult = FakeSamsungPaySdkState.StatusResult.Failure(
            errorCode = STATUS_ERROR,
            data = Bundle(),
        ),
    ) {
        val status = getStatus()

        assertThat(status).isInstanceOf(SamsungPayStatus.Failed::class.java)
        assertThat((status as SamsungPayStatus.Failed).error)
            .hasMessageThat()
            .contains(STATUS_ERROR.toString())
    }

    @Test
    fun `missing SDK reports failure and presentation does not crash`() = runScenario(
        classProvider = SamsungPayClassProvider { throw ClassNotFoundException(it) },
        expectAvailabilityAnalytics = false,
    ) {
        val status = getStatus()
        val result = present()

        assertThat(status).isInstanceOf(SamsungPayStatus.Failed::class.java)
        val statusError = (status as SamsungPayStatus.Failed).error as SamsungPaySdkException
        assertThat(statusError.reason)
            .isEqualTo(com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason.SdkUnavailable)
        assertThat(result).isInstanceOf(SamsungPayResult.Failed::class.java)
        assertThat((result as SamsungPayResult.Failed).error)
            .hasMessageThat()
            .contains("client app must include the Samsung Pay SDK JAR")
    }

    @Test
    fun `incompatible SDK reports actionable failure`() = runScenario(
        classProvider = SamsungPayClassProvider { className ->
            if (className == SamsungPaySdkClassNames.PARTNER_INFO) {
                String::class.java
            } else {
                fakeSamsungPaySdkClass(className)
            }
        },
        expectAvailabilityAnalytics = false,
    ) {
        val status = getStatus()

        assertThat(status).isInstanceOf(SamsungPayStatus.Failed::class.java)
        assertThat((status as SamsungPayStatus.Failed).error)
            .hasMessageThat()
            .contains("incompatible with the required 2.22.00 API")
    }

    @Test
    fun `payment sheet contains sanitized transaction and two-decimal amount`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)

        launchPayment()

        val statusPartnerInfo = requireNotNull(FakeSamsungPaySdkState.statusPartnerInfo)
        assertThat(statusPartnerInfo.serviceId).isEqualTo(DEFAULT_CONFIGURATION.serviceId)
        assertThat(statusPartnerInfo.data.getString(SpaySdk.PARTNER_SERVICE_TYPE))
            .isEqualTo(SpaySdk.ServiceType.INAPP_PAYMENT.toString())
        assertThat(statusPartnerInfo.data.getString(SpaySdk.PARTNER_SDK_API_LEVEL)).isEqualTo("2.22")

        val info = requireNotNull(FakeSamsungPaySdkState.paymentInfo)
        assertThat(info.merchantId).isEqualTo(DEFAULT_CONFIGURATION.merchantId)
        assertThat(info.merchantName).isEqualTo(DEFAULT_CONFIGURATION.merchantName)
        assertThat(info.orderNumber).isEqualTo("pi-123-secret-abc")
        assertThat(info.allowedCardBrands).containsExactly(
            SpaySdk.Brand.VISA,
            SpaySdk.Brand.MASTERCARD,
            SpaySdk.Brand.AMERICANEXPRESS,
            SpaySdk.Brand.DISCOVER,
        ).inOrder()
        assertThat(info.isCardHolderNameEnabled).isTrue()
        assertThat(info.isRecurring).isFalse()

        val amountControl = info.amountControl()
        assertThat(amountControl.currencyCode).isEqualTo("USD")
        assertThat(amountControl.total).isEqualTo(12.05)
        assertThat(amountControl.items.single().value).isEqualTo(12.05)
    }

    @Test
    fun `payment sheet omits merchant ID when not configured`() = runScenario(
        configuration = createConfiguration(merchantId = null),
    ) {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)

        launchPayment()

        assertThat(requireNotNull(FakeSamsungPaySdkState.paymentInfo).merchantId).isNull()
    }

    @Test
    fun `payment sheet uses Onramp merchant name when Samsung merchant name is omitted`() = runScenario(
        configuration = createConfiguration(merchantName = null),
        merchantDisplayName = "Onramp merchant",
    ) {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)

        launchPayment()

        assertThat(requireNotNull(FakeSamsungPaySdkState.paymentInfo).merchantName)
            .isEqualTo("Onramp merchant")
    }

    @Test
    fun `payment sheet preserves zero-decimal currency amount`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)

        launchPayment(
            SamsungPayPresentation(
                currencyCode = "jpy",
                amount = 1205,
                orderNumber = "order-123",
            ),
        )

        val amountControl = requireNotNull(FakeSamsungPaySdkState.paymentInfo).amountControl()
        assertThat(amountControl.currencyCode).isEqualTo("JPY")
        assertThat(amountControl.total).isEqualTo(1205.0)
    }

    @Test
    fun `card update sends updated sheet to payment manager`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        launchPayment()
        val customSheet = CustomSheet()

        requireNotNull(FakeSamsungPaySdkState.paymentListener)
            .onCardInfoUpdated(CardInfo(), customSheet)

        assertThat(FakeSamsungPaySdkState.updatedSheet).isSameInstanceAs(customSheet)
    }

    @Test
    fun `successful payment returns complete credential`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        var result: SamsungPayResult? = null
        launchPayment { result = it }

        requireNotNull(FakeSamsungPaySdkState.paymentListener).onSuccess(
            requireNotNull(FakeSamsungPaySdkState.paymentInfo),
            PAYMENT_CREDENTIAL,
            Bundle(),
        )

        assertThat(result).isEqualTo(SamsungPayResult.Completed(PAYMENT_CREDENTIAL))
        assertAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayObtainCredentialsSuccess)
    }

    @Test
    fun `payment callback received off main is delivered on main`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        var callbackLooper: Looper? = null
        launchPayment { callbackLooper = Looper.myLooper() }

        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit {
                requireNotNull(FakeSamsungPaySdkState.paymentListener).onSuccess(
                    requireNotNull(FakeSamsungPaySdkState.paymentInfo),
                    PAYMENT_CREDENTIAL,
                    Bundle(),
                )
            }.get()
            shadowOf(Looper.getMainLooper()).idle()
        } finally {
            executor.shutdownNow()
        }

        assertThat(callbackLooper).isSameInstanceAs(Looper.getMainLooper())
        assertAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayObtainCredentialsSuccess)
    }

    @Test
    fun `user canceled payment returns canceled`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        var result: SamsungPayResult? = null
        launchPayment { result = it }

        requireNotNull(FakeSamsungPaySdkState.paymentListener)
            .onFailure(SpaySdk.ERROR_USER_CANCELED, Bundle())

        assertThat(result).isEqualTo(SamsungPayResult.Canceled)
        assertAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayCanceled)
    }

    @Test
    fun `SDK payment failure preserves error code`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        var result: SamsungPayResult? = null
        launchPayment { result = it }

        requireNotNull(FakeSamsungPaySdkState.paymentListener).onFailure(PAYMENT_ERROR, Bundle())

        assertThat(result).isInstanceOf(SamsungPayResult.Failed::class.java)
        val paymentError = (result as SamsungPayResult.Failed).error as SamsungPaySdkException
        assertThat(paymentError.errorCode).isEqualTo(PAYMENT_ERROR)
        assertThat(paymentError.reason)
            .isEqualTo(com.stripe.android.crypto.onramp.exception.SamsungPayException.Reason.CredentialsFailed)
        assertAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayObtainCredentialsFailed(PAYMENT_ERROR))
    }

    @Test
    fun `duplicate presentation fails while original remains active`() = runScenario {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)
        launchPayment()

        val secondResult = present()

        assertThat(secondResult).isInstanceOf(SamsungPayResult.Failed::class.java)
        assertThat((secondResult as SamsungPayResult.Failed).error)
            .hasMessageThat()
            .contains("already in progress")
    }

    @Test
    fun `blank merchant name fails through result callback`() = runScenario(
        configuration = createConfiguration(merchantName = " "),
    ) {
        assertThat(getStatus()).isEqualTo(SamsungPayStatus.Ready)

        val result = present()

        assertThat(result).isInstanceOf(SamsungPayResult.Failed::class.java)
        assertThat((result as SamsungPayResult.Failed).error)
            .hasMessageThat()
            .contains("merchant name must not be blank")
    }

    private fun runScenario(
        configuration: OnrampConfiguration.SamsungPayConfig = DEFAULT_CONFIGURATION,
        merchantDisplayName: String = "Onramp merchant",
        classProvider: SamsungPayClassProvider = fakeSamsungPaySdkClassProvider,
        statusResult: FakeSamsungPaySdkState.StatusResult =
            FakeSamsungPaySdkState.StatusResult.Success(SpaySdk.SPAY_READY, Bundle()),
        expectAvailabilityAnalytics: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        FakeSamsungPaySdkState.reset()
        FakeSamsungPaySdkState.statusResult = statusResult
        val analyticsEvents = Turbine<OnrampAnalyticsEvent>()
        val launcher = DefaultSamsungPayLauncher(
            context = ApplicationProvider.getApplicationContext<Application>(),
            configuration = configuration,
            merchantDisplayName = merchantDisplayName,
            trackAnalyticsEvent = analyticsEvents::add,
            classProvider = classProvider,
        )
        assertAnalyticsEvent(
            actual = analyticsEvents.awaitItem(),
            expected = OnrampAnalyticsEvent.SamsungPayInitialized,
        )

        Scenario(
            launcher = launcher,
            analyticsEvents = analyticsEvents,
            statusResult = statusResult,
            expectAvailabilityAnalytics = expectAvailabilityAnalytics,
        ).apply { block() }

        launcher.destroy()
        analyticsEvents.ensureAllEventsConsumed()
    }

    private class Scenario(
        val launcher: DefaultSamsungPayLauncher,
        private val analyticsEvents: Turbine<OnrampAnalyticsEvent>,
        private val statusResult: FakeSamsungPaySdkState.StatusResult,
        private val expectAvailabilityAnalytics: Boolean,
    ) {
        suspend fun getStatus(): SamsungPayStatus {
            var result: SamsungPayStatus? = null
            launcher.getStatus { result = it }
            if (expectAvailabilityAnalytics) {
                val expectedStatus = when (statusResult) {
                    is FakeSamsungPaySdkState.StatusResult.Success -> statusResult.status
                    is FakeSamsungPaySdkState.StatusResult.Failure -> statusResult.errorCode
                }
                assertAnalyticsEvent(
                    actual = analyticsEvents.awaitItem(),
                    expected = OnrampAnalyticsEvent.SamsungPayAvailable(
                        available = expectedStatus == SpaySdk.SPAY_READY,
                        status = expectedStatus,
                    ),
                )
            }
            return requireNotNull(result)
        }

        suspend fun launchPayment(
            presentation: SamsungPayPresentation = DEFAULT_PRESENTATION,
            callback: (SamsungPayResult) -> Unit = {},
        ) {
            launcher.present(presentation, callback)
            assertAnalyticsEvent(OnrampAnalyticsEvent.SamsungPayPresented)
        }

        suspend fun assertAnalyticsEvent(expected: OnrampAnalyticsEvent) {
            assertAnalyticsEvent(
                actual = analyticsEvents.awaitItem(),
                expected = expected,
            )
        }

        fun present(
            presentation: SamsungPayPresentation = DEFAULT_PRESENTATION,
        ): SamsungPayResult {
            var result: SamsungPayResult? = null
            launcher.present(presentation) { result = it }
            return requireNotNull(result)
        }
    }

    private companion object {
        fun assertAnalyticsEvent(
            actual: OnrampAnalyticsEvent,
            expected: OnrampAnalyticsEvent,
        ) {
            assertThat(actual.eventName).isEqualTo(expected.eventName)
            assertThat(actual.params.orEmpty()).containsExactlyEntriesIn(expected.params.orEmpty())
        }

        const val UNKNOWN_NOT_READY_REASON = -999
        const val STATUS_ERROR = -301
        const val PAYMENT_ERROR = -103
        const val PAYMENT_CREDENTIAL = "{\"method\":\"3DS\"}"
        val DEFAULT_CONFIGURATION = createConfiguration()

        fun createConfiguration(
            merchantId: String? = "merchant_123",
            merchantName: String? = "Example merchant",
        ) = OnrampConfiguration.SamsungPayConfig(
            serviceId = "service_123",
            merchantId = merchantId,
            merchantName = merchantName,
            allowedCardBrands = listOf(
                CardBrand.Visa,
                CardBrand.MasterCard,
                CardBrand.AmericanExpress,
                CardBrand.Discover,
            ),
        )
        val DEFAULT_PRESENTATION = SamsungPayPresentation(
            currencyCode = "usd",
            amount = 1205,
            orderNumber = "pi_123_secret_abc",
        )

        fun notReadyStatus(reason: Int): FakeSamsungPaySdkState.StatusResult {
            return FakeSamsungPaySdkState.StatusResult.Success(
                status = SpaySdk.SPAY_NOT_READY,
                data = Bundle().apply { putInt(SamsungPay.EXTRA_ERROR_REASON, reason) },
            )
        }

        fun CustomSheetPaymentInfo.amountControl(): AmountBoxControl {
            return requireNotNull(customSheet).controls.single() as AmountBoxControl
        }
    }
}
