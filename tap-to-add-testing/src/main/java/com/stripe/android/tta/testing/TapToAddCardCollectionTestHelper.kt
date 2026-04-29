package com.stripe.android.tta.testing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.stripeterminal.external.models.AllowRedisplay
import com.stripe.stripeterminal.external.models.CollectSetupIntentConfiguration
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderSupportResult
import com.stripe.stripeterminal.external.models.SetupIntent
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import com.stripe.stripeterminal.external.models.TapUseCase

class TapToAddCardCollectionTestHelper(
    private val networkRule: NetworkRule,
    private val delegateRetriever: () -> TerminalTestDelegate,
) {
    private val delegate: TerminalTestDelegate
        get() = delegateRetriever()

    fun enqueueSuccessfulTapToCollectFlow(
        shouldValidate: Boolean = false,
        customerId: String = "cus_123"
    ): TapToCollectAssertionInfo {
        val reader = delegate.createReader()
        val generatedPaymentMethod = PaymentMethodFactory.card("pm_1")
        val cardPaymentMethod = PaymentMethodFactory.card("pm_2")
        val setupIntentClientSecret = "seti_123_secret_123"
        val setupIntent = delegate.createSetupIntent(customerId = customerId)
        val confirmedIntent = delegate.createSetupIntent(generatedPaymentMethod, customerId)

        delegate.setScenario(
            TerminalTestDelegate.Scenario(
                shouldValidate = shouldValidate,
                isInitialized = true,
                connectedReader = null,
                connectReaderResult = TerminalTestDelegate.ConnectReaderResult.Success(reader),
                discoveredReaders = listOf(reader),
                readerSupportResult = ReaderSupportResult.Supported,
                retrieveSetupIntentResult = TerminalTestDelegate.SetupIntentResult.Success(setupIntent),
                collectSetupIntentPaymentMethodResult =
                    TerminalTestDelegate.SetupIntentResult.Success(setupIntent),
                confirmSetupIntentResult =
                    TerminalTestDelegate.SetupIntentResult.Success(confirmedIntent),
            )
        )

        networkRule.enqueue(
            generatedCardToPaymentMethodRequest(
                customerId = "cus_123",
                pmId = generatedPaymentMethod.id,
            )
        ) { response ->
            response.setBody(PaymentMethodFactory.convertCardToJson(cardPaymentMethod).toString())
        }

        return TapToCollectAssertionInfo(
            reader = reader,
            setupIntent = setupIntent,
            cardPaymentMethod = cardPaymentMethod,
            setupIntentClientSecret = setupIntentClientSecret,
        )
    }

    suspend fun assertSuccessfulCardCollection(collect: TapToCollectAssertionInfo) {
        assertIsInitializedCall()
        assertConnectedReaderCall()
        assertSupportsReadersOfTypeCall()
        assertDiscoverCall()
        assertConnectReaderCall(collect.reader)
        assertSetTapToPayUxConfigurationCall()
        assertRetrieveSetupIntentCall(collect.setupIntentClientSecret)
        assertCollectSetupIntentPaymentMethod(collect.setupIntent)
        assertConfirmSetupIntent(collect.setupIntent)
    }

    private suspend fun assertIsInitializedCall() {
        assertThat(delegate.awaitIsInitializedCall()).isNotNull()
    }

    private suspend fun assertConnectedReaderCall() {
        assertThat(delegate.awaitConnectedReaderCall()).isNotNull()
    }

    private suspend fun assertDiscoverCall() {
        val discoverReadersCall = delegate.awaitDiscoverReadersCall()

        assertThat(discoverReadersCall.config)
            .isInstanceOf(DiscoveryConfiguration.TapToPayDiscoveryConfiguration::class.java)
    }

    private suspend fun assertSupportsReadersOfTypeCall() {
        val supportsReadersOfTypeCall = delegate.awaitSupportsReadersOfTypeCall()

        assertThat(supportsReadersOfTypeCall.deviceType).isEqualTo(DeviceType.TAP_TO_PAY_DEVICE)
        assertThat(supportsReadersOfTypeCall.discoveryConfiguration)
            .isInstanceOf(DiscoveryConfiguration.TapToPayDiscoveryConfiguration::class.java)
    }

    private suspend fun assertConnectReaderCall(reader: Reader) {
        val connectReaderCall = delegate.awaitConnectReaderCall()

        assertThat(connectReaderCall.reader).isEqualTo(reader)
        assertThat(connectReaderCall.config)
            .isInstanceOf(ConnectionConfiguration.TapToPayConnectionConfiguration::class.java)

        val connectionConfiguration =
            connectReaderCall.config as ConnectionConfiguration.TapToPayConnectionConfiguration

        assertThat(connectionConfiguration.useCase).isInstanceOf(TapUseCase.Verify::class.java)
    }

    private suspend fun assertSetTapToPayUxConfigurationCall() {
        val uxConfiguration = delegate.awaitSetTapToPayUxConfigurationCall()

        assertThat(uxConfiguration).isEqualTo(
            TapToPayUxConfiguration.Builder()
                .darkMode(darkMode = TapToPayUxConfiguration.DarkMode.SYSTEM)
                .colors(
                    colors = TapToPayUxConfiguration.ColorScheme.Builder()
                        .primary(
                            primary = TapToPayUxConfiguration.Color.Value(
                                color = Color(PRIMARY_COLOR).toArgb(),
                            )
                        )
                        .build()
                )
                .build()
        )
    }

    private suspend fun assertRetrieveSetupIntentCall(clientSecret: String) {
        val retrieveSetupIntentCall = delegate.awaitRetrieveSetupIntentCall()

        assertThat(retrieveSetupIntentCall.clientSecret).isEqualTo(clientSecret)
    }

    private suspend fun assertCollectSetupIntentPaymentMethod(intent: SetupIntent) {
        val collectSetupIntentPaymentMethodCall = delegate.awaitCollectSetupIntentPaymentMethodCall()

        assertThat(collectSetupIntentPaymentMethodCall.intent).isEqualTo(intent)
        assertThat(collectSetupIntentPaymentMethodCall.allowRedisplay)
            .isEqualTo(AllowRedisplay.ALWAYS)
        assertThat(collectSetupIntentPaymentMethodCall.config)
            .isEqualTo(CollectSetupIntentConfiguration.Builder().build())
    }

    private suspend fun assertConfirmSetupIntent(intent: SetupIntent) {
        val confirmSetupIntentCall = delegate.awaitConfirmSetupIntentCall()

        assertThat(confirmSetupIntentCall.intent).isEqualTo(intent)
    }

    private fun generatedCardToPaymentMethodRequest(
        customerId: String,
        pmId: String,
    ) = RequestMatchers.composite(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/elements/customers/$customerId/saved_payment_method_from_card_present_payment_method/$pmId"),
    )

    data class TapToCollectAssertionInfo(
        val reader: Reader,
        val setupIntent: SetupIntent,
        val cardPaymentMethod: PaymentMethod,
        val setupIntentClientSecret: String,
    )

    private companion object {
        const val PRIMARY_COLOR = 0xFF007AFF
    }
}
