package com.stripe.android.paymentsheet.example.playground.checkout

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.example.playground.checkout.settings.AdaptivePricingCountry
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.session
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class CheckoutSessionFactoryTest {
    @Test
    fun `guest creates only a Checkout Session`() = runScenario {
        val result = factory.create(settings.snapshot())

        assertThat(result).isEqualTo(CheckoutControllerExampleBackendResponse("cs_test", null))
        assertThat(backend.checkoutSessionCalls.awaitItem()["customer"]).isNull()
        backend.createCustomerCalls.expectNoEvents()
        paymentMethodCreator.calls.expectNoEvents()
        backend.attachPaymentMethodCalls.expectNoEvents()
        assertThat(interactions.awaitItem()).isEqualTo("create_checkout_session")
    }

    @Test
    fun `new customer is created with adaptive pricing email`() = runScenario {
        settings.update(session.customer, CheckoutCustomer.New)
        settings.update(session.customerEmail, "ordinary@example.com")
        settings.update(session.adaptivePricingCountry, AdaptivePricingCountry.France)

        val result = factory.create(settings.snapshot())

        assertThat(backend.createCustomerCalls.awaitItem().getValue("email").toString())
            .isEqualTo("\"test+location_FR@example.com\"")
        assertThat(backend.checkoutSessionCalls.awaitItem().getValue("customer").toString()).isEqualTo("\"cus_new\"")
        assertThat(result.customerId).isEqualTo("cus_new")
        assertThat(listOf(interactions.awaitItem(), interactions.awaitItem()))
            .containsExactly("create_customer", "create_checkout_session").inOrder()
    }

    @Test
    fun `returning customer reuses configured ID`() = runScenario {
        settings.update(session.customer, CheckoutCustomer.Returning)
        settings.update(session.customerId, "cus_saved")

        val result = factory.create(settings.snapshot())

        assertThat(backend.checkoutSessionCalls.awaitItem().getValue("customer").toString()).isEqualTo("\"cus_saved\"")
        assertThat(result.customerId).isEqualTo("cus_saved")
        backend.createCustomerCalls.expectNoEvents()
        paymentMethodCreator.calls.expectNoEvents()
        backend.attachPaymentMethodCalls.expectNoEvents()
        assertThat(interactions.awaitItem()).isEqualTo("create_checkout_session")
    }

    @Test
    fun `returning customer without ID is created then seeded before session`() = runScenario {
        settings.update(session.customer, CheckoutCustomer.Returning)

        val result = factory.create(settings.snapshot())

        assertThat(backend.createCustomerCalls.awaitItem()).isNotNull()
        val paymentMethodParams = paymentMethodCreator.calls.awaitItem()
        assertThat(paymentMethodParams.toParamMap()["allow_redisplay"]).isEqualTo("always")
        assertThat(paymentMethodParams.toParamMap()["card"]).isEqualTo(
            mapOf("number" to "4242424242424242", "exp_month" to 12, "exp_year" to 2030, "cvc" to "123")
        )
        assertThat(backend.attachPaymentMethodCalls.awaitItem()).isEqualTo(
            AttachPaymentMethodCall(paymentMethodId = "pm_4242", customerId = "cus_new")
        )
        assertThat(backend.checkoutSessionCalls.awaitItem().getValue("customer").toString()).isEqualTo("\"cus_new\"")
        assertThat(
            listOf(
                interactions.awaitItem(),
                interactions.awaitItem(),
                interactions.awaitItem(),
                interactions.awaitItem(),
            )
        ).containsExactly(
            "create_customer",
            "create_payment_method",
            "attach_payment_method",
            "create_checkout_session",
        ).inOrder()
        assertThat(result.customerId).isEqualTo("cus_new")
    }

    @Test
    fun `customer creation failure is propagated and stops workflow`() = runScenario(
        createCustomerError = IllegalStateException("Customer failed"),
    ) {
        settings.update(session.customer, CheckoutCustomer.New)

        val result = runCatching { factory.create(settings.snapshot()) }

        assertThat(result.exceptionOrNull()).hasMessageThat().isEqualTo("Customer failed")
        assertThat(backend.createCustomerCalls.awaitItem()).isNotNull()
        paymentMethodCreator.calls.expectNoEvents()
        backend.attachPaymentMethodCalls.expectNoEvents()
        backend.checkoutSessionCalls.expectNoEvents()
        assertThat(interactions.awaitItem()).isEqualTo("create_customer")
    }

    private fun runScenario(
        createCustomerError: Throwable? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val interactions = Turbine<String>()
        val backend = FakeCheckoutPlaygroundBackend(interactions, createCustomerError)
        val paymentMethodCreator = FakePlaygroundPaymentMethodCreator(interactions)
        Scenario(
            settings = CheckoutPlaygroundSettings.createInMemory(),
            backend = backend,
            paymentMethodCreator = paymentMethodCreator,
            interactions = interactions,
            factory = CheckoutSessionFactory(backend, paymentMethodCreator),
        ).apply { block() }
        backend.ensureAllEventsConsumed()
        paymentMethodCreator.ensureAllEventsConsumed()
        interactions.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val settings: CheckoutPlaygroundSettings,
        val backend: FakeCheckoutPlaygroundBackend,
        val paymentMethodCreator: FakePlaygroundPaymentMethodCreator,
        val interactions: Turbine<String>,
        val factory: CheckoutSessionFactory,
    )
}

private class FakeCheckoutPlaygroundBackend(
    private val interactions: Turbine<String>,
    private val createCustomerError: Throwable?,
) : CheckoutPlaygroundBackend {
    val createCustomerCalls = Turbine<JsonObject>()
    val attachPaymentMethodCalls = Turbine<AttachPaymentMethodCall>()
    val checkoutSessionCalls = Turbine<JsonObject>()

    override suspend fun fetchPublishableKey(): String = error("Not used")

    override suspend fun createCustomer(requestParams: JsonObject): String {
        createCustomerCalls.add(requestParams)
        interactions.add("create_customer")
        createCustomerError?.let { throw it }
        return "cus_new"
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String, customerId: String) {
        attachPaymentMethodCalls.add(AttachPaymentMethodCall(paymentMethodId, customerId))
        interactions.add("attach_payment_method")
    }

    override suspend fun createCheckoutSession(requestParams: JsonObject): String {
        checkoutSessionCalls.add(requestParams)
        interactions.add("create_checkout_session")
        return "cs_test"
    }

    fun ensureAllEventsConsumed() {
        createCustomerCalls.ensureAllEventsConsumed()
        attachPaymentMethodCalls.ensureAllEventsConsumed()
        checkoutSessionCalls.ensureAllEventsConsumed()
    }
}

private class FakePlaygroundPaymentMethodCreator(
    private val interactions: Turbine<String>,
) : PlaygroundPaymentMethodCreator {
    val calls = Turbine<PaymentMethodCreateParams>()

    override suspend fun createPaymentMethod(params: PaymentMethodCreateParams): String {
        calls.add(params)
        interactions.add("create_payment_method")
        return "pm_4242"
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}

private data class AttachPaymentMethodCall(
    val paymentMethodId: String,
    val customerId: String,
)
