package com.stripe.android.ui.core.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.testing.FakeErrorReporter
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExternalPaymentMethodsRepositoryTest {

    @Test
    fun `externalPaymentMethodData is null, result is empty list`() {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter)
        val externalPaymentMethodData = null
        val expectedEpms = emptyList<ExternalPaymentMethodSpec>()

        val actualEpms = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(externalPaymentMethodData)

        assertThat(actualEpms).isEqualTo(expectedEpms)
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `externalPaymentMethodData is empty string, result is empty list`() {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter)
        val externalPaymentMethodData = ""
        val expectedEpms = emptyList<ExternalPaymentMethodSpec>()

        val actualEpms = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(externalPaymentMethodData)

        assertThat(actualEpms).isEqualTo(expectedEpms)
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `externalPaymentMethodData is invalid, result is empty list`() = runTest {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter)
        val externalPaymentMethodData = "invalid_input!!"
        val expectedEpms = emptyList<ExternalPaymentMethodSpec>()

        val actualEpms = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(externalPaymentMethodData)

        assertThat(actualEpms).isEqualTo(expectedEpms)
        assertThat(errorReporter.awaitCall().errorEvent)
            .isEqualTo(ErrorReporter.UnexpectedErrorEvent.EXTERNAL_PAYMENT_METHOD_SERIALIZATION_FAILURE)
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `externalPaymentMethod contains one EPM, result is one EPM`() {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter)
        val externalPaymentMethodData = VENMO_EXTERNAL_PAYMENT_METHOD_DATA
        val expectedEpms = listOf(VENMO_EPM)

        val actualEpms = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(externalPaymentMethodData)

        assertThat(actualEpms).isEqualTo(expectedEpms)
        errorReporter.ensureAllEventsConsumed()
    }

    @Test
    fun `externalPaymentMethod contains two EPMs, result is two EPMs`() {
        val errorReporter = FakeErrorReporter()
        val externalPaymentMethodsRepository = ExternalPaymentMethodsRepository(errorReporter)
        val externalPaymentMethodData = PAYPAL_AND_VENMO_EXTERNAL_PAYMENT_METHOD_DATA
        val expectedEpms = listOf(VENMO_EPM, PAYPAL_EPM)

        val actualEpms = externalPaymentMethodsRepository.getExternalPaymentMethodSpecs(externalPaymentMethodData)

        assertThat(actualEpms).isEqualTo(expectedEpms)
        errorReporter.ensureAllEventsConsumed()
    }

    private val VENMO_EPM = ExternalPaymentMethodSpec(
        type = "external_venmo",
        label = "Venmo",
        darkImageUrl = null,
        lightImageUrl = "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-epm-venmo-" +
            "162b3cf0020c8fe2ce4bde7ec3845941.png",
    )

    private val PAYPAL_EPM = ExternalPaymentMethodSpec(
        type = "external_paypal",
        label = "PayPal",
        darkImageUrl = "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-paypal_dark@" +
            "3x-26040e151c8f87187da2f997791fcc31.png",
        lightImageUrl = "https://js.stripe.com/v3/fingerprinted/img/payment-methods/icon-pm-paypal@" +
            "3x-5227ab4fca3d36846bd6622f495cdf4b.png",

    )

    private val VENMO_EXTERNAL_PAYMENT_METHOD_DATA = """
        [
            {
                "dark_image_url": null,
                "label": "Venmo",
                "light_image_url": "https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-epm-venmo-162b3cf0020c8fe2ce4bde7ec3845941.png",
                "type": "external_venmo"
            }
        ]        
    """

    private val PAYPAL_AND_VENMO_EXTERNAL_PAYMENT_METHOD_DATA = """
       [
            {
                "dark_image_url":null,
                "label":"Venmo",
                "light_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-epm-venmo-162b3cf0020c8fe2ce4bde7ec3845941.png",
                "type":"external_venmo"
            },
            {
                "dark_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-paypal_dark@3x-26040e151c8f87187da2f997791fcc31.png",
                "label":"PayPal",
                "light_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-paypal@3x-5227ab4fca3d36846bd6622f495cdf4b.png",
                "type":"external_paypal"
            }
        ] 
    """.trimIndent()
}
