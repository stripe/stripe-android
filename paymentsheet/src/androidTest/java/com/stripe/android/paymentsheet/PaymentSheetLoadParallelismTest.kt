package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Collections
import java.util.concurrent.TimeUnit

/**
 * Tests that verify network requests during PaymentSheet loading are parallelized as expected.
 *
 * If a test in this class fails because previously sequential requests are removed or now in parallel, yay! You likely
 * just improved our loading latency.
 *
 * If a test in this class fails because previously parallel requests become sequential, please evaluate whether this is
 * necessary and intentional, as it will likely lead to an increase in our loading latency.
 *
 */
@RunWith(JUnit4::class)
internal class PaymentSheetLoadParallelismTest {

    @get:Rule
    val testRules: TestRules = TestRules.create()

    private val networkRule = testRules.networkRule
    private val page: PaymentSheetPage = PaymentSheetPage(testRules.compose)

    private val requestLog: MutableList<RequestEvent> =
        Collections.synchronizedList(mutableListOf())

    private data class RequestEvent(val request: Request, val arrivalNanos: Long)

    companion object {
        /**
         * The body delay applied to each mock response. If requests are truly parallel, they
         * should all arrive at the mock server within a small window of each other regardless
         * of this delay. If they are serial, subsequent requests won't arrive until at least
         * DELAY_MS after the previous response.
         */
        private const val DELAY_MS = 100L

        /**
         * Maximum allowed time gap (in ms) between requests that should arrive concurrently.
         * This is generous to accommodate CI variability.
         */
        private const val PARALLEL_TOLERANCE_MS = 80L
    }

    @Test
    fun testLinkOffWithNoCustomer() = runLoadTest(
        customerType = CustomerType.Guest,
        linkEnabled = false,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Singleton(request = Request.ElementsSessionsWithCs),
    )

    @Test
    fun testLinkOffWithEk() = runLoadTest(
        customerType = CustomerType.LegacyEK,
        linkEnabled = false,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Sequential(
                requests = listOf(
                    RequestOrdering.Parallel(
                        requests = listOf(
                            Request.ElementsSessionsWithEk,
                            Request.FetchCards,
                            Request.FetchSepaDebit,
                            Request.FetchUsBank,
                        )
                    ),
                    RequestOrdering.Singleton(request = Request.FetchCustomer)
                )
            ),
    )

    @Test
    fun testLinkOffWithCs() = runLoadTest(
        customerType = CustomerType.CustomerSession,
        linkEnabled = false,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Singleton(request = Request.ElementsSessionsWithCs),
    )

    @Test
    fun testLinkOnWithNoCustomer() = runLoadTest(
        customerType = CustomerType.Guest,
        linkEnabled = true,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Singleton(request = Request.ElementsSessionsWithCs),
    )

    @Test
    fun testLinkOnWithEk() = runLoadTest(
        customerType = CustomerType.LegacyEK,
        linkEnabled = true,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Sequential(
            requests = listOf(
                RequestOrdering.Parallel(
                    requests = listOf(
                        Request.ElementsSessionsWithEk,
                        Request.FetchCards,
                        Request.FetchSepaDebit,
                        Request.FetchUsBank,
                    )
                ),
                RequestOrdering.Singleton(request = Request.FetchCustomer)
            )
        ),
    )

    @Test
    fun testLinkOnWithCs() = runLoadTest(
        customerType = CustomerType.CustomerSession,
        linkEnabled = true,
        defaultEmail = false,
        expectedRequestOrdering = RequestOrdering.Singleton(request = Request.ElementsSessionsWithCs),
    )

    @Test
    fun testLinkOnWithEkDefaultEmail() = runLoadTest(
        customerType = CustomerType.LegacyEK,
        linkEnabled = true,
        defaultEmail = true,
        expectedRequestOrdering = RequestOrdering.Sequential(
            requests = listOf(
                RequestOrdering.Parallel(
                    requests = listOf(
                        Request.ElementsSessionsWithEk,
                        Request.FetchCards,
                        Request.FetchSepaDebit,
                        Request.FetchUsBank,
                    )
                ),
                RequestOrdering.Singleton(request = Request.LinkAccountLookup),
            )
        ),
    )

    @Test
    fun testLinkOnWithCsDefaultEmail() = runLoadTest(
        customerType = CustomerType.CustomerSession,
        linkEnabled = true,
        defaultEmail = true,
        RequestOrdering.Sequential(
            requests = listOf(
                RequestOrdering.Singleton(request = Request.ElementsSessionsWithCs),
                RequestOrdering.Singleton(request = Request.LinkAccountLookup),
            )
        ),
    )

    private enum class CustomerType {
        Guest,
        LegacyEK,
        CustomerSession,
    }

    private fun runLoadTest(
        customerType: CustomerType,
        linkEnabled: Boolean,
        defaultEmail: Boolean,
        expectedRequestOrdering: RequestOrdering,
    ) = runPaymentSheetTest(
        networkRule = networkRule,
        resultCallback = ::expectNoResult,
        successTimeoutSeconds = 15L,
    ) { testContext ->
        enqueueLoadResponses(
            requests = expectedRequestOrdering.allRequests(),
            linkEnabled = linkEnabled,
        )

        testContext.presentPaymentSheet {
            presentWithPaymentIntent(
                paymentIntentClientSecret = "pi_example_secret_example",
                configuration = buildConfiguration(
                    customerType = customerType,
                    defaultEmail = defaultEmail,
                ),
            )
        }

        page.waitForCardForm()

        assertParallelismForConfig(expectedRequestOrdering)

        testContext.markTestSucceeded()
    }

    private fun enqueueLoadResponses(
        requests: List<Request>,
        linkEnabled: Boolean,
    ) {
        requests.forEach { request ->
            when (request) {
                Request.ElementsSessionsWithCs -> enqueueElementsSessionWithCustomerSessionsRequest(linkEnabled)
                Request.ElementsSessionsWithEk -> enqueueElementsSessionWithEphemeralKeyRequest(linkEnabled)
                Request.FetchCards -> enqueueFetchPaymentMethods(PaymentMethod.Type.Card, request)
                Request.FetchSepaDebit -> enqueueFetchPaymentMethods(PaymentMethod.Type.SepaDebit, request)
                Request.FetchUsBank -> enqueueFetchPaymentMethods(PaymentMethod.Type.USBankAccount, request)
                Request.FetchCustomer -> enqueueFetchCustomer()
                Request.LinkAccountLookup -> enqueueLinkAccountLookup()
            }
        }
    }

    private fun enqueueElementsSessionWithCustomerSessionsRequest(
        linkEnabled: Boolean,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { _, response ->
            recordArrival(Request.ElementsSessionsWithCs)
            response.setBodyDelay(DELAY_MS, TimeUnit.MILLISECONDS)
            if (linkEnabled) {
                response.testBodyFromFile("elements-sessions-requires_pm_with_ps_pi_cs.json") { json ->
                    val linkSettings = json.optJSONObject("link_settings")
                        ?: JSONObject()
                    linkSettings.put("link_passthrough_mode_enabled", true)
                    json.put("link_settings", linkSettings)
                }
            } else {
                response.testBodyFromFile("elements-sessions-requires_pm_with_ps_pi_cs.json")
            }
        }
    }

    private fun enqueueElementsSessionWithEphemeralKeyRequest(
        linkEnabled: Boolean,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        ) { _, response ->
            recordArrival(Request.ElementsSessionsWithEk)
            response.setBodyDelay(DELAY_MS, TimeUnit.MILLISECONDS)
            if (linkEnabled) {
                response.testBodyFromFile( "elements-sessions-requires_payment_method.json") { json ->
                    val linkSettings = json.optJSONObject("link_settings")
                        ?: JSONObject()
                    linkSettings.put("link_passthrough_mode_enabled", true)
                    json.put("link_settings", linkSettings)
                }
            } else {
                response.testBodyFromFile( "elements-sessions-requires_payment_method.json")
            }
        }
    }

    private fun enqueueFetchPaymentMethods(
        type: PaymentMethod.Type,
        request: Request,
    ) {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
            query("type", type.code),
        ) { _, response ->
            recordArrival(request)
            response.setBodyDelay(DELAY_MS, TimeUnit.MILLISECONDS)
            response.setBody(
                """{"object":"list","data":[],"has_more":false,"url":"/v1/payment_methods"}"""
            )
        }
    }

    private fun enqueueFetchCustomer() {
        networkRule.enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/customers/cus_1"),
        ) { _, response ->
            recordArrival(Request.FetchCustomer)
            response.setBodyDelay(DELAY_MS, TimeUnit.MILLISECONDS)
            response.setBody(
                """{"id":"cus_1","object":"customer","email":null,"name":null}"""
            )
        }
    }

    private fun enqueueLinkAccountLookup() {
        networkRule.enqueue(
            method("POST"),
            path("/v1/consumers/sessions/lookup"),
        ) { _, response ->
            recordArrival(Request.LinkAccountLookup)
            response.setBodyDelay(DELAY_MS, TimeUnit.MILLISECONDS)
            response.setBody("""{"exists":"false"}""")
        }
    }

    private fun buildConfiguration(
        customerType: CustomerType,
        defaultEmail: Boolean,
    ): PaymentSheet.Configuration {
        return PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
            customer = when (customerType) {
                CustomerType.Guest -> null
                CustomerType.LegacyEK -> PaymentSheet.CustomerConfiguration(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_123",
                )
                CustomerType.CustomerSession -> PaymentSheet.CustomerConfiguration
                    .createWithCustomerSession(
                        id = "cus_1",
                        clientSecret = "cuss_123",
                    )
            },
            defaultBillingDetails = if (defaultEmail) {
                PaymentSheet.BillingDetails(email = "test@example.com")
            } else {
                null
            },
        )
    }

    private fun assertParallelismForConfig(
        requestOrdering: RequestOrdering,
    ) {
        when (requestOrdering) {
            is RequestOrdering.Parallel -> assertParallelRequests(requestOrdering)
            is RequestOrdering.Sequential -> assertSequentialRequests(requestOrdering)
            is RequestOrdering.Singleton -> {} // The fact that the network request occurred is enough for these.
        }
    }

    private fun assertSequentialRequests(
        requestOrdering: RequestOrdering.Sequential,
    ) {
        if (requestOrdering.requests.size < 2) {
            return
        }

        (0..<requestOrdering.requests.size - 1).forEach { idx ->
            val firstRequest = requestOrdering.requests[idx].firstRequest()
            val secondRequest = requestOrdering.requests[idx + 1].firstRequest()
            val firstArrival = findArrival(firstRequest)
            val secondArrival = findArrival(secondRequest)
            val deltaMs = (secondArrival - firstArrival) / 1_000_000
            if (deltaMs < DELAY_MS - 20) {
                throw AssertionError(
                    "$secondRequest was expected to be ${DELAY_MS - 20}ms after $firstRequest but was $deltaMs"
                )
            }
            assertThat(deltaMs).isAtLeast(DELAY_MS - 20)
        }
    }

    private fun recordArrival(request: Request) {
        requestLog.add(RequestEvent(request, System.nanoTime()))
    }

    private fun findArrival(request: Request): Long {
        val event = requestLog.firstOrNull { it.request == request }
            ?: throw AssertionError("Expected request '$request' to have been recorded")
        return event.arrivalNanos
    }

    /**
     * Asserts that all requests with the given labels arrived at the mock server within
     * [PARALLEL_TOLERANCE_MS] of each other, indicating they were fired in parallel.
     */
    private fun assertParallelRequests(parallelRequests: RequestOrdering.Parallel) {
        val arrivals = parallelRequests.requests.map { request -> findArrival(request) }

        val minArrival = arrivals.min()
        val maxArrival = arrivals.max()
        val spreadMs = (maxArrival - minArrival) / 1_000_000

        if (spreadMs > PARALLEL_TOLERANCE_MS) {
            throw AssertionError(
                "Requests (${parallelRequests.requests.joinToString { it.name }}) should arrive concurrently. " +
                    "Spread was ${spreadMs}ms, max allowed is ${PARALLEL_TOLERANCE_MS}ms."
            )
        }
    }

    private enum class Request {
        ElementsSessionsWithCs,
        ElementsSessionsWithEk,
        FetchCards,
        FetchSepaDebit,
        FetchUsBank,
        FetchCustomer,
        LinkAccountLookup,
    }

    private sealed interface RequestOrdering {

        data class Singleton(val request: Request) : RequestOrdering

        data class Parallel(val requests: List<Request>) : RequestOrdering

        data class Sequential(val requests: List<RequestOrdering>) : RequestOrdering

        fun allRequests(): List<Request> {
            return when (this) {
                is Parallel -> this.requests
                is Sequential -> this.requests.map { it.allRequests() }.flatten()
                is Singleton -> listOf(this.request)
            }
        }

        fun firstRequest(): Request {
            return this.allRequests()[0]
        }
    }
}
