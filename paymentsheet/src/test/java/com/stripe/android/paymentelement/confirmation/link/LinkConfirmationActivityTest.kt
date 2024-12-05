package com.stripe.android.paymentelement.confirmation.link

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentElementConfirmationTestActivity
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.assertComplete
import com.stripe.android.paymentelement.confirmation.assertConfirming
import com.stripe.android.paymentelement.confirmation.assertIdle
import com.stripe.android.paymentelement.confirmation.assertSucceeded
import com.stripe.android.payments.paymentlauncher.InternalPaymentResult
import com.stripe.android.paymentsheet.createTestActivityRule
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.verticalmode.ShampooRule
import com.stripe.android.testing.FeatureFlagTestRule
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class LinkConfirmationActivityTest(private val nativeLinkEnabled: Boolean) {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @get:Rule
    val intentsRule = IntentsRule()

    @get:Rule
    val testActivityRule = createTestActivityRule<PaymentElementConfirmationTestActivity>()

    @get:Rule
    val paymentConfigurationTestRule = PaymentConfigurationTestRule(
        context = application,
    )

    @get:Rule
    val featureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkEnabled,
        isEnabled = false
    )

    @get:Rule
    val shampooRule = ShampooRule(200)

    @Test
    fun linkSucceeds() = test {
        val paymentMethod = PaymentMethodFactory.card()

        confirmationHandler.state.test {
            awaitItem().assertIdle()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    intent = PAYMENT_INTENT,
                    confirmationOption = LINK_CONFIRMATION_OPTION,
                )
            )

            intendingLinkToBeLaunched(
                LINK_COMPLETE_CODE,
            ) {
                val convertedPaymentMethod = String(
                    Base64.encode(
                        PaymentMethodFactory
                            .convertCardToJson(paymentMethod)
                            .toString(2)
                            .encodeToByteArray(),
                        0
                    ),
                    Charsets.UTF_8,
                )

                setData(
                    Uri.parse("https://link.com/redirect?link_status=complete&pm=$convertedPaymentMethod")
                )
            }

            intendingPaymentConfirmationToBeLaunched(
                InternalPaymentResult.Completed(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            )

            val confirmingWithLink = awaitItem().assertConfirming()

            assertThat(confirmingWithLink.option).isEqualTo(LINK_CONFIRMATION_OPTION)

            intendedLinkToBeLaunched()

            val confirmingWithSavedPaymentMethod = awaitItem().assertConfirming()

            assertThat(confirmingWithSavedPaymentMethod.option)
                .isEqualTo(
                    PaymentMethodConfirmationOption.Saved(
                        initializationMode = LINK_CONFIRMATION_OPTION.initializationMode,
                        shippingDetails = LINK_CONFIRMATION_OPTION.shippingDetails,
                        paymentMethod = paymentMethod,
                        optionsParams = null,
                    )
                )

            intendedPaymentConfirmationToBeLaunched()

            val successResult = awaitItem().assertComplete().result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT.copy(paymentMethod = paymentMethod))
            assertThat(successResult.deferredIntentConfirmationType).isNull()
        }
    }

    private fun test(
        test: suspend PaymentElementConfirmationTestActivity.() -> Unit
    ) = runTest(UnconfinedTestDispatcher()) {
        featureFlagTestRule.setEnabled(nativeLinkEnabled)

        val countDownLatch = CountDownLatch(1)

        ActivityScenario.launch<PaymentElementConfirmationTestActivity>(
            Intent(application, PaymentElementConfirmationTestActivity::class.java)
        ).use { scenario ->
            scenario.onActivity { activity ->
                launch {
                    test(activity)

                    countDownLatch.countDown()
                }
            }

            countDownLatch.await(10, TimeUnit.SECONDS)
        }
    }

    private fun intendingLinkToBeLaunched(
        resultCode: Int,
        applyToIntent: Intent.() -> Unit,
    ) {
        if (FeatureFlags.nativeLinkEnabled.isEnabled) {
            intending(hasComponent(LINK_ACTIVITY_NAME)).respondWith(
                Instrumentation.ActivityResult(
                    resultCode,
                    Intent().apply {
                        applyToIntent()
                    }
                )
            )
        } else {
            intending(hasComponent(LINK_FOREGROUND_ACTIVITY_NAME)).respondWith(
                Instrumentation.ActivityResult(
                    resultCode,
                    Intent().apply {
                        applyToIntent()
                    }
                )
            )
        }
    }

    private fun intendingPaymentConfirmationToBeLaunched(result: InternalPaymentResult) {
        intending(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME)).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtras(bundleOf("extra_args" to result))
            )
        )
    }

    private fun intendedLinkToBeLaunched() {
        if (FeatureFlags.nativeLinkEnabled.isEnabled) {
            intended(hasComponent(LINK_ACTIVITY_NAME))
        } else {
            intended(hasComponent(LINK_FOREGROUND_ACTIVITY_NAME))
        }
    }

    private fun intendedPaymentConfirmationToBeLaunched() {
        intended(hasComponent(PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME))
    }

    private companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "Native Link Enabled: {0}")
        fun params() = listOf(
            arrayOf(true),
            arrayOf(false),
        )

        val PAYMENT_INTENT = PaymentIntentFactory.create().copy(
            id = "pm_1",
            amount = 5000,
            currency = "CAD",
        )

        val LINK_CONFIRMATION_OPTION = LinkConfirmationOption(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "pi_123_secret_123"
            ),
            shippingDetails = null,
            configuration = LinkConfiguration(
                stripeIntent = PAYMENT_INTENT,
                merchantName = "Merchant, Inc.",
                merchantCountryCode = "CA",
                customerInfo = LinkConfiguration.CustomerInfo(
                    name = "John Doe",
                    email = "johndoe@email.com",
                    phone = "+1234567890",
                    billingCountryCode = "CA",
                ),
                shippingValues = mapOf(),
                passthroughModeEnabled = false,
                flags = mapOf(),
                cardBrandChoice = null,
            ),
        )

        const val LINK_COMPLETE_CODE = 49871

        const val LINK_ACTIVITY_NAME =
            "com.stripe.android.link.LinkActivity"

        const val LINK_FOREGROUND_ACTIVITY_NAME =
            "com.stripe.android.link.LinkForegroundActivity"

        const val PAYMENT_CONFIRMATION_LAUNCHER_ACTIVITY_NAME =
            "com.stripe.android.payments.paymentlauncher.PaymentLauncherConfirmationActivity"
    }
}
