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
    fun blikToParamMap_withCode_includeCode() {
        val blikCode = "123456"
        assertThat(
            PaymentMethodOptionsParams.Blik(
                code = blikCode
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                PaymentMethod.Type.Blik.code to mapOf(
                    PaymentMethodOptionsParams.Blik.PARAM_CODE to blikCode
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
    fun setupFutureUsageToParamMap_hasCorrectValues() {
        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.SepaDebit,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "sepa_debit" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )

        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.WeChatPay,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "wechat_pay" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )

        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.Link,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "link" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )

        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.Card,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )

        assertThat(
            PaymentMethodOptionsParams.SetupFutureUsage(
                paymentMethodType = PaymentMethod.Type.Blik,
                setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OnSession
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "blik" to mapOf(
                    "setup_future_usage" to "on_session"
                )
            )
        )
    }
}
