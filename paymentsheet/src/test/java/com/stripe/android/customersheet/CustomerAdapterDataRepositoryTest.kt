package com.stripe.android.customersheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.data.CustomerAdapterDataRepository
import com.stripe.android.customersheet.data.CustomerPaymentOption
import com.stripe.android.customersheet.data.CustomerSheetSession
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerAdapterDataRepositoryTest {
    @Test
    fun `On 'loadCustomerSession', should call for elements session & payment methods`() = runTest {
        var calledAdapter = false

        val paymentMethodTypes = listOf("card")
        val adapter = FakeCustomerAdapter(
            paymentMethodTypes = paymentMethodTypes,
            onRetrievePaymentMethods = {
                calledAdapter = true
                CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD,
                    )
                )
            },
        )

        val elementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            error = null,
            linkSettings = null,
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(
            adapter = adapter,
            elementsSessionRepository = elementsSessionRepository,
        )

        val result = customerAdapterDataRepository.loadCustomerSheetSession()

        assertThat(calledAdapter).isTrue()
        assertThat(elementsSessionRepository.lastParams?.initializationMode).isInstanceOf(
            PaymentSheet.InitializationMode.DeferredIntent::class.java
        )
        assertThat(result.getOrNull()).isEqualTo(
            CustomerSheetSession(
                elementsSession = ElementsSession(
                    stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
                    linkSettings = null,
                    customer = null,
                    externalPaymentMethodData = null,
                    paymentMethodSpecs = null,
                    merchantCountry = null,
                    isGooglePayEnabled = true,
                    isEligibleForCardBrandChoice = false,
                ),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD,
                )
            )
        )
    }

    @Test
    fun `On 'retrievePaymentMethods', should call 'CustomerAdapter' for payment methods`() = runTest {
        var called = false

        val adapter = FakeCustomerAdapter(
            onRetrievePaymentMethods = {
                called = true
                CustomerAdapter.Result.success(
                    listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD,
                    )
                )
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.retrievePaymentMethods()

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).containsExactly(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD,
        )
    }

    @Test
    fun `On 'attachPaymentMethod', should call 'CustomerAdapter' to attach payment method`() = runTest {
        var called = false
        val attachingId = "pm_12341234"
        val attachedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = attachingId)

        val adapter = FakeCustomerAdapter(
            onAttachPaymentMethod = {
                called = true
                CustomerAdapter.Result.success(attachedPaymentMethod)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.attachPaymentMethod(attachingId)

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).isEqualTo(attachedPaymentMethod)
    }

    @Test
    fun `On 'detachPaymentMethod', should call 'CustomerAdapter' to detach payment method`() = runTest {
        var called = false
        val removingId = "pm_12341234"
        val removedPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = removingId)

        val adapter = FakeCustomerAdapter(
            onDetachPaymentMethod = {
                called = true
                CustomerAdapter.Result.success(removedPaymentMethod)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.detachPaymentMethod(removingId)

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).isEqualTo(removedPaymentMethod)
    }

    @Test
    fun `On 'updatePaymentMethod', should call 'CustomerAdapter' to update payment method`() = runTest {
        var called = false
        val updatingId = "pm_12341234"
        val originalPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = updatingId)
        val updatedPaymentMethod = originalPaymentMethod.copy(
            card = originalPaymentMethod.card?.copy(
                expiryMonth = 2,
                expiryYear = 2031,
            ),
        )

        val adapter = FakeCustomerAdapter(
            onUpdatePaymentMethod = { _, _ ->
                called = true
                CustomerAdapter.Result.success(updatedPaymentMethod)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.updatePaymentMethod(
            updatingId,
            PaymentMethodUpdateParams.createCard(
                expiryMonth = 2,
                expiryYear = 2031,
            ),
        )

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).isEqualTo(updatedPaymentMethod)
    }

    @Test
    fun `On 'setSelectedPaymentOption', should call 'CustomerAdapter' to set selected payment option`() = runTest {
        var called = false

        val adapter = FakeCustomerAdapter(
            onSetSelectedPaymentOption = {
                called = true
                CustomerAdapter.Result.success(Unit)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.setSelectedPaymentOption(
            CustomerPaymentOption.GooglePay
        )

        assertThat(called).isTrue()
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `On 'retrieveSelectedPaymentOption', should call 'CustomerAdapter' for selected payment option`() = runTest {
        var called = false

        val adapter = FakeCustomerAdapter(
            onGetSelectedPaymentOption = {
                called = true
                CustomerAdapter.Result.success(CustomerAdapter.PaymentOption.GooglePay)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.retrieveSelectedPaymentOption()

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).isEqualTo(CustomerPaymentOption.GooglePay)
    }

    @Test
    fun `On 'retrieveSetupIntentClientSecret', should call 'CustomerAdapter' to for client secret`() = runTest {
        var called = false
        val setupIntentId = "seti_123"

        val adapter = FakeCustomerAdapter(
            onSetupIntentClientSecretForCustomerAttach = {
                called = true
                CustomerAdapter.Result.success(setupIntentId)
            },
        )

        val customerAdapterDataRepository = createCustomerAdapterDataRepository(adapter)

        val result = customerAdapterDataRepository.retrieveSetupIntentClientSecret()

        assertThat(called).isTrue()
        assertThat(result.getOrNull()).isEqualTo(setupIntentId)
    }

    private suspend fun createCustomerAdapterDataRepository(
        adapter: CustomerAdapter,
        elementsSessionRepository: ElementsSessionRepository = FakeElementsSessionRepository(
            stripeIntent = SetupIntentFixtures.SI_REQUIRES_PAYMENT_METHOD,
            linkSettings = null,
            error = null,
        ),
    ): CustomerAdapterDataRepository {
        return CustomerAdapterDataRepository(
            workContext = coroutineContext,
            adapter = adapter,
            elementsSessionRepository = elementsSessionRepository,
        )
    }
}
