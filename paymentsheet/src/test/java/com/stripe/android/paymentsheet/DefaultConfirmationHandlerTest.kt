package com.stripe.android.paymentsheet

import android.content.Context
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.payments.paymentlauncher.PaymentLauncherContract
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncher
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultConfirmationHandlerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val fakeClientSecret = PaymentIntentClientSecret(value = "secret")

    @Test
    fun `Succeeds payment handling when provided with a valid payment selection`() = runTest {
        val fakeActivityResultCaller = FakeActivityResultCaller(result = PaymentResult.Completed)

        val paymentHandler = createPaymentHandler()
        paymentHandler.registerFromActivity(fakeActivityResultCaller)

        val result = paymentHandler.confirm(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.createCard(),
                isGooglePay = false,
            ),
            clientSecret = fakeClientSecret,
            shipping = null,
        )

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `Fails payment handling when provided with an invalid payment selection`() = runTest {
        val fakeActivityResultCaller = FakeActivityResultCaller()

        val paymentHandler = createPaymentHandler()
        paymentHandler.registerFromActivity(fakeActivityResultCaller)

        val result = paymentHandler.confirm(
            paymentSelection = PaymentSelection.Link,
            clientSecret = fakeClientSecret,
            shipping = null,
        )

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `Fails payment handling when launcher returns failure`() = runTest {
        val failure = PaymentResult.Failed(RuntimeException("whoopsie daisy"))
        val fakeActivityResultCaller = FakeActivityResultCaller(result = failure)

        val paymentHandler = createPaymentHandler()
        paymentHandler.registerFromActivity(fakeActivityResultCaller)

        val result = paymentHandler.confirm(
            paymentSelection = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.createCard(),
                isGooglePay = false,
            ),
            clientSecret = fakeClientSecret,
            shipping = null,
        )

        assertThat(result.getOrThrow()).isEqualTo(failure)
    }

    private fun createPaymentHandler(): ConfirmationHandler {
        return DefaultConfirmationHandler(
            paymentLauncherFactory = createPaymentLauncherFactory(),
            lazyPaymentConfig = {
                PaymentConfiguration(
                    publishableKey = "public_key",
                    stripeAccountId = null,
                )
            }
        )
    }

    private fun createPaymentLauncherFactory(): StripePaymentLauncherAssistedFactory {
        return object : StripePaymentLauncherAssistedFactory {
            override fun create(
                publishableKey: () -> String,
                stripeAccountId: () -> String?,
                hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
            ): StripePaymentLauncher {
                return createPaymentLauncher(hostActivityLauncher)
            }
        }
    }

    private fun createPaymentLauncher(
        hostActivityLauncher: ActivityResultLauncher<PaymentLauncherContract.Args>
    ): StripePaymentLauncher {
        return StripePaymentLauncher(
            publishableKeyProvider = { "public_key" },
            stripeAccountIdProvider = { null },
            hostActivityLauncher = hostActivityLauncher,
            context = context,
            enableLogging = false,
            ioContext = Dispatchers.IO,
            uiContext = Dispatchers.Main,
            stripeRepository = mock(),
            paymentAnalyticsRequestFactory = mock(),
            productUsage = emptySet(),
        )
    }
}

private class FakeActivityResultCaller private constructor(
    private val fakeRegistry: FakeActivityResultRegistry,
) : ActivityResultCaller {

    constructor(result: PaymentResult? = null) : this(
        fakeRegistry = FakeActivityResultRegistry(result),
    )

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        return FakeActivityResultLauncher(contract, fakeRegistry, callback)
    }

    override fun <I : Any?, O : Any?> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        return FakeActivityResultLauncher(contract, fakeRegistry, callback)
    }
}

private class FakeActivityResultRegistry(
    val result: PaymentResult?
) : ActivityResultRegistry() {
    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?
    ) {
        dispatchResult(requestCode, result)
    }
}

private class FakeActivityResultLauncher<I : Any?, O : Any?>(
    private val contract: ActivityResultContract<I, O>,
    private val registry: FakeActivityResultRegistry,
    private val callback: ActivityResultCallback<O>,
) : ActivityResultLauncher<I>() {

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        val result = registry.result as? O
        callback.onActivityResult(result)
    }

    override fun unregister() {
        // Nothing to do here
    }

    override fun getContract() = contract
}
