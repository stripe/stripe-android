package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.StripeIntentValidator
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
internal class DeferredIntentRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var deferredIntentRepository: DeferredIntentRepository

    private val stripeRepository = mock<StripeRepository>()

    @Before
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        deferredIntentRepository = DefaultDeferredIntentRepository(
            lazyPaymentConfig = { PaymentConfiguration.getInstance(context) },
            stripeRepository = stripeRepository,
            elementsSessionRepository = ElementsSessionRepository.Static(
                PaymentIntentFixtures.PI_WITH_SHIPPING
            ),
            stripeIntentValidator = StripeIntentValidator()
        )
    }

    @Test
    fun `get returns success with client secret and is confirmed`() = runTest {
        whenever(stripeRepository.createPaymentMethod(any(), any()))
            .thenReturn(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 12345,
                    currency = "usd"
                )
            )
        )

        val result = deferredIntentRepository.get(
            paymentSelection = paymentSelection,
            initializationMode = initializationMode,
            confirmCallback = ConfirmCallbackForServerSideConfirmation { _, _ ->
                ConfirmCallback.Result.Success(
                    clientSecret = PaymentIntentFixtures.PI_WITH_SHIPPING.clientSecret.orEmpty()
                )
            }
        )

        assertThat(result)
            .isEqualTo(
                DeferredIntentRepository.Result.Success(
                    clientSecret = PaymentIntentClientSecret(
                        PaymentIntentFixtures.PI_WITH_SHIPPING.clientSecret.orEmpty()
                    ),
                    isConfirmed = true
                )
            )
    }

    @Test
    fun `get returns success with client secret and is not confirmed`() = runTest {
        whenever(stripeRepository.createPaymentMethod(any(), any()))
            .thenReturn(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 12345,
                    currency = "usd"
                )
            )
        )

        val result = deferredIntentRepository.get(
            paymentSelection = paymentSelection,
            initializationMode = initializationMode,
            confirmCallback = ConfirmCallbackForClientSideConfirmation {
                ConfirmCallback.Result.Success(
                    clientSecret = PaymentIntentFixtures.PI_WITH_SHIPPING.clientSecret.orEmpty()
                )
            }
        )

        assertThat(result)
            .isEqualTo(
                DeferredIntentRepository.Result.Success(
                    clientSecret = PaymentIntentClientSecret(
                        PaymentIntentFixtures.PI_WITH_SHIPPING.clientSecret.orEmpty()
                    ),
                    isConfirmed = false
                )
            )
    }

    @Test
    fun `get returns error from integrator server`() = runTest {
        whenever(stripeRepository.createPaymentMethod(any(), any()))
            .thenReturn(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 12345,
                    currency = "usd"
                )
            )
        )

        val result = deferredIntentRepository.get(
            paymentSelection = paymentSelection,
            initializationMode = initializationMode,
            confirmCallback = ConfirmCallbackForClientSideConfirmation {
                ConfirmCallback.Result.Failure(
                    error = "Some error"
                )
            }
        )

        assertThat(result)
            .isEqualTo(
                DeferredIntentRepository.Result.Error(
                    error = "Some error"
                )
            )
    }

    @Test
    fun `get throws error when payment method id cannot be retrieved`() = runTest {
        val exception = assertFails {
            deferredIntentRepository.get(
                paymentSelection = mock(),
                initializationMode = mock(),
                confirmCallback = null
            )
        }

        assertThat(exception.message)
            .isEqualTo("A valid payment method ID is required for the DeferredIntent flow")
    }

    @Test
    fun `get throws error when integrator does not implement callback`() = runTest {
        whenever(stripeRepository.createPaymentMethod(any(), any()))
            .thenReturn(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )

        val paymentSelection = PaymentSelection.New.Card(
            PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
            CardBrand.Visa,
            customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
        )

        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                    amount = 12345,
                    currency = "usd"
                )
            )
        )

        val exception = assertFails {
            deferredIntentRepository.get(
                paymentSelection = paymentSelection,
                initializationMode = initializationMode,
                confirmCallback = null
            )
        }

        assertThat(exception.message)
            .isEqualTo("ConfirmCallback must be implemented for DeferredIntent flow")
    }
}
