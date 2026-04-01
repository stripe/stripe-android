package com.stripe.android.common.taptoadd

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.CreateIntentResult
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

@OptIn(TapToAddPreview::class)
class TapToAddCardCollectionTestHelper(
    private val networkRule: NetworkRule,
    private val imageLoaderTestRule: TapToAddStripeImageLoaderTestRule,
    private val terminalWrapperTestRule: TerminalWrapperTestRule,
) {
    val paymentElementCallbackIdentifier = PAYMENT_ELEMENT_CALLBACK_IDENTIFIER

    fun enqueueSuccessfulTapToCollectFlow(): TapToCollectAssertionInfo {
        val reader = terminalWrapperTestRule.createReader()
        val generatedPaymentMethod = PaymentMethodFactory.card("pm_1")
        val cardPaymentMethod = PaymentMethodFactory.card("pm_2")
        val setupIntentClientSecret = "seti_123_secret_123"
        val setupIntent = terminalWrapperTestRule.createSetupIntent()
        val confirmedIntent = terminalWrapperTestRule.createSetupIntent(generatedPaymentMethod)

        enqueueCallbacks(
            createCardPresentSetupIntentCallback = {
                CreateIntentResult.Success(setupIntentClientSecret)
            },
        )

        terminalWrapperTestRule.enqueueScenario(
            TerminalWrapperTestRule.Scenario(
                isInitialized = true,
                connectedReader = null,
                connectReaderResult = TerminalWrapperTestRule.ConnectReaderResult.Success(reader),
                discoveredReaders = listOf(reader),
                readerSupportResult = ReaderSupportResult.Supported,
                retrieveSetupIntentResult = TerminalWrapperTestRule.SetupIntentResult.Success(setupIntent),
                collectSetupIntentPaymentMethodResult =
                    TerminalWrapperTestRule.SetupIntentResult.Success(setupIntent),
                confirmSetupIntentResult =
                    TerminalWrapperTestRule.SetupIntentResult.Success(confirmedIntent),
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
        assertCardArtAssetPreloads()
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

    private suspend fun assertCardArtAssetPreloads() {
        /*
         * These images are loaded asynchronously all together. The order itself does not matter, only that all the
         * images were asked to be loaded.
         */
        val preloadingImages = setOf(
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
            imageLoaderTestRule.awaitImageLoadWithUrl(),
        )

        assertThat(preloadingImages).containsExactly(
            "https://b.stripecdn.com/ocs-mobile/assets/visa.png",
            "https://b.stripecdn.com/ocs-mobile/assets/mastercard.png",
            "https://b.stripecdn.com/ocs-mobile/assets/discover.webp",
            "https://b.stripecdn.com/ocs-mobile/assets/amex.webp",
            "https://b.stripecdn.com/ocs-mobile/assets/jcb.png"
        )
    }

    private suspend fun assertIsInitializedCall() {
        assertThat(terminalWrapperTestRule.awaitIsInitializedCall()).isNotNull()
    }

    private suspend fun assertConnectedReaderCall() {
        assertThat(terminalWrapperTestRule.awaitConnectedReaderCall()).isNotNull()
    }

    private suspend fun assertDiscoverCall() {
        val discoverReadersCall = terminalWrapperTestRule.awaitDiscoverReadersCall()

        assertThat(discoverReadersCall.config)
            .isInstanceOf<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>()
    }

    private suspend fun assertSupportsReadersOfTypeCall() {
        val supportsReadersOfTypeCall = terminalWrapperTestRule.awaitSupportsReadersOfTypeCall()

        assertThat(supportsReadersOfTypeCall.deviceType).isEqualTo(DeviceType.TAP_TO_PAY_DEVICE)
        assertThat(supportsReadersOfTypeCall.discoveryConfiguration)
            .isInstanceOf<DiscoveryConfiguration.TapToPayDiscoveryConfiguration>()
    }

    private suspend fun assertConnectReaderCall(reader: Reader) {
        val connectReaderCall = terminalWrapperTestRule.awaitConnectReaderCall()

        assertThat(connectReaderCall.reader).isEqualTo(reader)
        assertThat(connectReaderCall.config)
            .isInstanceOf<ConnectionConfiguration.TapToPayConnectionConfiguration>()

        val connectionConfiguration =
            connectReaderCall.config as ConnectionConfiguration.TapToPayConnectionConfiguration

        assertThat(connectionConfiguration.useCase).isInstanceOf<TapUseCase.Verify>()
    }

    private suspend fun assertSetTapToPayUxConfigurationCall() {
        val uxConfiguration = terminalWrapperTestRule.awaitSetTapToPayUxConfigurationCall()

        assertThat(uxConfiguration).isEqualTo(
            TapToPayUxConfiguration.Builder()
                .darkMode(darkMode = TapToPayUxConfiguration.DarkMode.SYSTEM)
                .colors(
                    colors = TapToPayUxConfiguration.ColorScheme.Builder()
                        .primary(
                            primary = TapToPayUxConfiguration.Color.Value(
                                color = Color(0xFF007AFF).toArgb(),
                            )
                        )
                        .build()
                )
                .build()
        )
    }

    private suspend fun assertRetrieveSetupIntentCall(clientSecret: String) {
        val retrieveSetupIntentCall = terminalWrapperTestRule.awaitRetrieveSetupIntentCall()

        assertThat(retrieveSetupIntentCall.clientSecret).isEqualTo(clientSecret)
    }

    private suspend fun assertCollectSetupIntentPaymentMethod(intent: SetupIntent) {
        val collectSetupIntentPaymentMethodCall = terminalWrapperTestRule.awaitCollectSetupIntentPaymentMethodCall()

        assertThat(collectSetupIntentPaymentMethodCall.intent).isEqualTo(intent)
        assertThat(collectSetupIntentPaymentMethodCall.allowRedisplay).isEqualTo(AllowRedisplay.ALWAYS)
        assertThat(collectSetupIntentPaymentMethodCall.config).isEqualTo(
            CollectSetupIntentConfiguration.Builder().build()
        )
    }

    private suspend fun assertConfirmSetupIntent(intent: SetupIntent) {
        val confirmSetupIntentCall = terminalWrapperTestRule.awaitConfirmSetupIntentCall()

        assertThat(confirmSetupIntentCall.intent).isEqualTo(intent)
    }

    private fun enqueueCallbacks(
        createCardPresentSetupIntentCallback: CreateCardPresentSetupIntentCallback,
    ) {
        PaymentElementCallbackReferences[PAYMENT_ELEMENT_CALLBACK_IDENTIFIER] = PaymentElementCallbacks.Builder()
            .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)
            .build()
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
        const val PAYMENT_ELEMENT_CALLBACK_IDENTIFIER = "mpe1"
    }
}
