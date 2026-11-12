package com.stripe.android.checkout.webauth

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.UriMatchers.hasScheme
import androidx.test.espresso.intent.rule.IntentsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CHECKOUT_PLAYGROUND_BACKEND_URL
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.checkout.CheckoutPlaygroundBackend
import com.stripe.android.checkout.CheckoutSessionParamsBuilder
import com.stripe.android.checkout.runCheckoutIntegrationTest
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedContentPage
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(CheckoutSessionPreview::class)
@RunWith(AndroidJUnit4::class)
internal class CheckoutRedirectIntegrationTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val intentsRule = IntentsRule()

    private val contentPage = EmbeddedContentPage(composeRule)

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
    fun testRedirectReturnWithoutAuthorizationReportsCanceled() = runBlocking {
        // The device has no browser, so stub the tab StripeBrowserLauncherActivity opens. Returning without
        // authorizing is the same as closing it.
        intending(BROWSER_INTENT).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))

        val backend = CheckoutPlaygroundBackend(
            baseUrl = CHECKOUT_PLAYGROUND_BACKEND_URL,
            merchant = "US",
        )
        val result = runCheckoutIntegrationTest(
            backend = backend,
            sessionParams = CheckoutSessionParamsBuilder(unitAmount = AFFIRM_MINIMUM_AMOUNT)
                .allowedPaymentMethodTypes(listOf("card", "affirm"))
                .build(),
            configuration = configuration,
            setup = {
                contentPage.clickOnLpm("affirm")
                contentPage.assertHasSelectedLpm("affirm")
            },
            handleNextAction = {},
        )

        assertThat(result).isInstanceOf(CheckoutElementsController.Result.Canceled::class.java)
        // Pins the StripeBrowserLauncherActivity route selected by the default return URL.
        val intents = getIntents()
        assertThat(intents.any { hasComponent(STRIPE_BROWSER_LAUNCHER_ACTIVITY).matches(it) }).isTrue()
        assertThat(intents.any { BROWSER_INTENT.matches(it) }).isTrue()
    }

    private companion object {
        const val STRIPE_BROWSER_LAUNCHER_ACTIVITY =
            "com.stripe.android.payments.StripeBrowserLauncherActivity"

        const val AFFIRM_MINIMUM_AMOUNT = 5000L

        // The auth URL comes from the live backend, so match any https browser launch.
        val BROWSER_INTENT = allOf(hasAction(Intent.ACTION_VIEW), hasData(hasScheme("https")))
    }
}
