package com.stripe.android.checkout.webauth

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CHECKOUT_PLAYGROUND_BACKEND_URL
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutPlaygroundBackend
import com.stripe.android.checkout.CheckoutSessionParamsBuilder
import com.stripe.android.checkout.runCheckoutIntegrationTest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import com.stripe.android.paymentelement.EmbeddedFormPage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// CI runs this package on a Google APIs emulator, apart from the aosp-atd suite whose WebView 101 leaks Activities.
@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutWebAuthIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val contentPage = EmbeddedContentPage(composeRule)
    private val formPage = EmbeddedFormPage(composeRule)
    private val challengePage = ThreeDS2ChallengePage(
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    )

    private val configuration = CheckoutElementsController.Configuration()
        .merchantDisplayName("Checkout E2E")
        .paymentElement(
            PaymentElement.Configuration()
                .googlePayConfiguration(
                    PaymentElement.Configuration.GooglePayConfiguration()
                        .display(PaymentElement.Configuration.GooglePayConfiguration.Display.Never)
                )
                .linkConfiguration(
                    PaymentElement.Configuration.LinkConfiguration()
                        .display(PaymentElement.Configuration.LinkConfiguration.Display.Never)
                )
        )

    @Test
    fun testCardWith3ds2ChallengeCompletes() = runBlocking {
        val result = runCheckoutIntegrationTest(
            backend = CheckoutPlaygroundBackend(
                baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
                merchant = "US",
            ),
            sessionParams = CheckoutSessionParamsBuilder(unitAmount = 1000L).build(),
            configuration = configuration,
            setup = {
                contentPage.clickOnLpm("card")
                formPage.fillOutCardDetails(newCardNumber = "4000000000003220")
                formPage.clickPrimaryButton()
            },
            handleNextAction = {
                challengePage.complete()
            },
        )
        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Completed::class.java)
    }
}
