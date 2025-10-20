package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PaymentMethodOptionsParamsTest {

    @Test
    fun cardToParamMap_withNetwork_shouldOnlyIncludeNetwork() {
        assertThat(
            PaymentMethodOptionsParams.Card(
                network = "visa"
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "network" to "visa"
                )
            )
        )
    }

    @Test
    fun blikToParamMap_hasCorrectValues() {
        val blikCode = "123456"
        assertThat(
            PaymentMethodOptionsParams.Blik(
                code = blikCode,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.None
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                PaymentMethod.Type.Blik.code to mapOf(
                    PaymentMethodOptionsParams.Blik.PARAM_CODE to blikCode,
                    PaymentMethodOptionsParams.Blik.PARAM_SETUP_FUTURE_USAGE to "none"
                )
            )
        )
    }

    @Test
    fun cardToParamMap_withNoData_shouldHaveEmptyParams() {
        assertThat(
            PaymentMethodOptionsParams.Card()
                .toParamMap()
        ).isEmpty()
    }

    @Test
    fun usBankAccountToParamMap_withNoData_shouldHaveEmptyParams() {
        assertThat(
            PaymentMethodOptionsParams.USBankAccount()
                .toParamMap()
        ).isEmpty()
    }

    @Test
    fun sepaDebitToParamMap_withSetupFutureUsage_shouldIncludeSetupFutureUsage() {
        assertThat(
            PaymentMethodOptionsParams.SepaDebit(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "sepa_debit" to mapOf(
                    "setup_future_usage" to "off_session"
                )
            )
        )
    }

    @Test
    fun sepaDebitToParamMap_withNoData_shouldHaveEmptyParams() {
        assertThat(
            PaymentMethodOptionsParams.SepaDebit()
                .toParamMap()
        ).isEmpty()
    }

    @Test
    fun sepaDebitToParamMap_withSetupFutureUsageOnSession_shouldIncludeSetupFutureUsage() {
        assertThat(
            PaymentMethodOptionsParams.SepaDebit(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "sepa_debit" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )
    }

    @Test
    fun weChatPayToParamMap_hasCorrectValues() {
        assertThat(
            PaymentMethodOptionsParams.WeChatPay(
                appId = "some_id",
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.None
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "wechat_pay" to mapOf(
                    PaymentMethodOptionsParams.WeChatPay.PARAM_CLIENT to "android",
                    PaymentMethodOptionsParams.WeChatPay.PARAM_APP_ID to "some_id",
                    PaymentMethodOptionsParams.WeChatPay.PARAM_SETUP_FUTURE_USAGE to "none",
                )
            )
        )
    }

    @Test
    fun weChatPayH5ToParamMap_hasCorrectValues() {
        assertThat(
            PaymentMethodOptionsParams.WeChatPayH5(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.None
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "wechat_pay" to mapOf(
                    PaymentMethodOptionsParams.WeChatPay.PARAM_CLIENT to "mobile_web",
                    PaymentMethodOptionsParams.WeChatPay.PARAM_SETUP_FUTURE_USAGE to "none"
                )
            )
        )
    }

    @Test
    fun konbiniToParamMap_hasCorrectValues() {
        assertThat(
            PaymentMethodOptionsParams.Konbini(
                confirmationNumber = "12345",
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.None
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "konbini" to mapOf(
                    PaymentMethodOptionsParams.Konbini.PARAM_CONFIRMATION_NUMBER to "12345",
                    PaymentMethodOptionsParams.Konbini.PARAM_SETUP_FUTURE_USAGE to "none"
                )
            )
        )
    }

    @Test
    fun setupFutureUsageToParamMap_hasCorrectValues() {
        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.Klarna,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.None
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "klarna" to mapOf(
                    PaymentMethodOptionsParams.SetupFutureUsage.PARAM_SETUP_FUTURE_USAGE to "none"
                )
            )
        )
    }

    @Test
    fun updateSetupFutureUsageWithPmoSfu_setsCorrectSfuValue() {
        val params = PaymentMethodOptionsParams.SepaDebit(
            setupFutureUsage = null
        )

        val newParams = params.updateSetupFutureUsageWithPmoSfu(
            pmoSfu = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )

        assertThat(newParams).isEqualTo(
            PaymentMethodOptionsParams.SepaDebit(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        )
    }

    @Test
    fun updateSetupFutureUsageWithoutPmoSfu_setsCorrectSfu() {
        val params = PaymentMethodOptionsParams.SepaDebit(
            setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
        )

        val newParams = params.updateSetupFutureUsageWithPmoSfu(
            pmoSfu = ConfirmPaymentIntentParams.SetupFutureUsage.None
        )

        assertThat(newParams).isEqualTo(
            PaymentMethodOptionsParams.SepaDebit(
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
            )
        )
    }
}
