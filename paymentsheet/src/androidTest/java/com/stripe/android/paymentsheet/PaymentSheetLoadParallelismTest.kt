package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.utils.TestRules
import com.stripe.android.paymentsheet.utils.expectNoResult
import com.stripe.android.paymentsheet.utils.runPaymentSheetTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.Closeable
import java.util.concurrent.CountDownLatch
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

    companion object {
        private const val REQUEST_TIMEOUT_SECONDS = 10L
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
        expectedRequestOrdering = RequestOrdering.Parallel(
            requests = listOf(
                Request.ElementsSessionsWithEk,
                Request.FetchCards,
                Request.FetchSepaDebit,
                Request.FetchUsBank,
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
        runScenario(expectedRequestOrdering) {
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

            assertParallelismForConfig(expectedRequestOrdering)
            page.waitForCardForm()

            testContext.markTestSucceeded()
        }
    }

    private fun enableLinkPassthroughMode(json: JSONObject) {
        val linkSettings = json.optJSONObject("link_settings")
            ?: JSONObject()
        linkSettings.put("link_passthrough_mode_enabled", true)
        json.put("link_settings", linkSettings)
    }

    private fun setPaymentMethodTypes(
        json: JSONObject,
        paymentMethodTypes: List<PaymentMethod.Type>,
    ) {
        val paymentMethodsJson = JSONArray(paymentMethodTypes.map { it.code })
        json.put("ordered_payment_method_types_and_wallets", "card")
        val paymentMethodPreferences = json.getJSONObject("payment_method_preference")
        val paymentIntent = paymentMethodPreferences.getJSONObject("payment_intent")
        paymentIntent.put("payment_method_types", paymentMethodsJson)
        paymentMethodPreferences.put("payment_intent", paymentIntent)
        paymentMethodPreferences.put("ordered_payment_method_types", paymentMethodsJson)
        json.put("payment_method_preference", paymentMethodPreferences)
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

    private fun runScenario(
        expectedRequestOrdering: RequestOrdering,
        block: Scenario.() -> Unit,
    ) {
        Scenario(expectedRequestOrdering).use(block)
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
    }

    private inner class Scenario(
        expectedRequestOrdering: RequestOrdering,
    ) : Closeable {
        val requestTracker = RequestTracker(expectedRequestOrdering.allRequests())

        fun enqueueLoadResponses(
            requests: List<Request>,
            linkEnabled: Boolean,
        ) {
            requests.forEach { request ->
                when (request) {
                    Request.ElementsSessionsWithCs -> enqueueElementsSession(
                        "elements-sessions-requires_pm_with_ps_pi_cs.json",
                        request = request,
                        linkEnabled = linkEnabled,
                    )
                    Request.ElementsSessionsWithEk -> enqueueElementsSession(
                        "elements-sessions-requires_payment_method.json",
                        request = Request.ElementsSessionsWithEk,
                        linkEnabled = linkEnabled,
                    )
                    Request.FetchCards -> enqueueFetchPaymentMethods(PaymentMethod.Type.Card, request)
                    Request.FetchSepaDebit -> enqueueFetchPaymentMethods(PaymentMethod.Type.SepaDebit, request)
                    Request.FetchUsBank -> enqueueFetchPaymentMethods(PaymentMethod.Type.USBankAccount, request)
                    Request.FetchCustomer -> enqueueFetchCustomer()
                    Request.LinkAccountLookup -> enqueueLinkAccountLookup()
                }
            }
        }

        fun enqueueElementsSession(
            bodyFile: String,
            request: Request,
            linkEnabled: Boolean,
        ) {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/elements/sessions"),
            ) { _, response ->
                enqueueTrackedResponse(request) {
                    if (linkEnabled) {
                        response.testBodyFromFile(bodyFile) { json ->
                            enableLinkPassthroughMode(json)
                            setPaymentMethodTypes(
                                json, paymentMethodTypes = listOf(
                                    PaymentMethod.Type.Card,
                                    PaymentMethod.Type.Link
                                )
                            )
                        }
                    } else {
                        response.testBodyFromFile(bodyFile) { json ->
                            setPaymentMethodTypes(json, paymentMethodTypes = listOf(PaymentMethod.Type.Card))
                        }
                    }
                }
            }
        }

        fun enqueueFetchPaymentMethods(
            type: PaymentMethod.Type,
            request: Request,
        ) {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/payment_methods"),
                query("type", type.code),
            ) { _, response ->
                enqueueTrackedResponse(request) {
                    response.setBody(
                        """{"object":"list","data":[],"has_more":false,"url":"/v1/payment_methods"}"""
                    )
                }
            }
        }

        fun enqueueFetchCustomer() {
            networkRule.enqueue(
                host("api.stripe.com"),
                method("GET"),
                path("/v1/customers/cus_1"),
            ) { _, response ->
                enqueueTrackedResponse(Request.FetchCustomer) {
                    response.setBody(
                        """{"id":"cus_1","object":"customer","email":null,"name":null}"""
                    )
                }
            }
        }

        fun enqueueLinkAccountLookup() {
            networkRule.enqueue(
                method("POST"),
                path("/v1/consumers/sessions/lookup"),
            ) { _, response ->
                enqueueTrackedResponse(Request.LinkAccountLookup) {
                    response.setBody("""{"exists":"false"}""")
                }
            }
        }

        fun enqueueTrackedResponse(
            request: Request,
            block: () -> Unit,
        ) {
            requestTracker.recordStarted(request)
            requestTracker.awaitRelease(request)

            try {
                block()
            } finally {
                requestTracker.recordFinished(request)
            }
        }

        fun assertParallelismForConfig(
            requestOrdering: RequestOrdering,
        ) {
            when (requestOrdering) {
                is RequestOrdering.Parallel -> assertParallelRequests(requestOrdering)
                is RequestOrdering.Sequential -> assertSequentialRequests(requestOrdering)
                is RequestOrdering.Singleton -> assertSingletonRequest(requestOrdering)
            }
        }

        fun assertSingletonRequest(
            requestOrdering: RequestOrdering.Singleton,
        ) {
            requestTracker.awaitStarted(requestOrdering.request)
            assertNoOtherRequestsInProgress(setOf(requestOrdering.request))
            requestTracker.release(requestOrdering.request)
            requestTracker.awaitFinished(requestOrdering.request)
        }

        fun assertSequentialRequests(
            requestOrdering: RequestOrdering.Sequential,
        ) {
            requestOrdering.requests.forEach { nestedRequestOrdering ->
                assertParallelismForConfig(nestedRequestOrdering)
            }
        }

        fun assertNoOtherRequestsInProgress(
            currentRequests: Set<Request>,
        ) {
            val inProgressRequests = requestTracker.inProgressRequestsExcluding(currentRequests)

            if (inProgressRequests.isNotEmpty()) {
                throw AssertionError(
                    "Requests (${inProgressRequests.joinToString { it.name }}) were in progress outside the " +
                        "current request group (${currentRequests.joinToString { it.name }})."
                )
            }
        }

        fun assertParallelRequests(
            parallelRequests: RequestOrdering.Parallel,
        ) {
            requestTracker.awaitStarted(parallelRequests.requests)
            assertNoOtherRequestsInProgress(parallelRequests.requests.toSet())
            requestTracker.release(parallelRequests.requests)
            requestTracker.awaitFinished(parallelRequests.requests)
        }

        override fun close() {
            requestTracker.cleanup()
        }
    }

    private class RequestTracker(
        requests: List<Request>,
    ) {
        private val requestStates: Map<Request, RequestState>

        init {
            require(requests.size == requests.toSet().size) {
                "Each request must only be tracked once. Requests: ${requests.joinToString()}"
            }

            requestStates = requests.associateWith { RequestState() }
        }

        fun recordStarted(request: Request) {
            state(request).started.countDown()
        }

        fun recordFinished(request: Request) {
            state(request).finished.countDown()
        }

        fun awaitRelease(request: Request) {
            try {
                state(request).releaseResponse.await()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw AssertionError("Interrupted while waiting for response for '$request' to be released.", e)
            }
        }

        fun awaitStarted(request: Request) {
            awaitWithTimeout(
                latch = state(request).started,
                description = "request '$request' to start",
            )
        }

        fun awaitStarted(requests: List<Request>) {
            val deadlineNanos = deadlineFromNow()
            requests.forEach { request ->
                awaitUntil(
                    latch = state(request).started,
                    description = "request '$request' to start",
                    deadlineNanos = deadlineNanos,
                )
            }
        }

        fun awaitFinished(request: Request) {
            awaitWithTimeout(
                latch = state(request).finished,
                description = "request '$request' to finish",
            )
        }

        fun awaitFinished(requests: List<Request>) {
            val deadlineNanos = deadlineFromNow()
            requests.forEach { request ->
                awaitUntil(
                    latch = state(request).finished,
                    description = "request '$request' to finish",
                    deadlineNanos = deadlineNanos,
                )
            }
        }

        fun release(request: Request) {
            state(request).releaseResponse.countDown()
        }

        fun release(requests: List<Request>) {
            requests.forEach(::release)
        }

        fun releaseAll() {
            requestStates.values.forEach { requestState ->
                requestState.releaseResponse.countDown()
            }
        }

        fun cleanup() {
            releaseAll()

            try {
                awaitFinished(requestStates.keys.toList())
            } catch (e: AssertionError) {
                throw AssertionError(
                    "Unfinished requests at the end of the scenario: ${unfinishedRequests().joinToString()}. ${e.message}"
                )
            }

            validateNoUnfinishedRequests()
        }

        fun inProgressRequestsExcluding(requests: Set<Request>): List<Request> {
            return requestStates.keys.filter { request ->
                request !in requests && isInProgress(request)
            }
        }

        fun validateNoUnfinishedRequests() {
            val unfinishedRequests = unfinishedRequests()
            if (unfinishedRequests.isNotEmpty()) {
                throw AssertionError(
                    "Unfinished requests at the end of the scenario: ${unfinishedRequests.joinToString()}."
                )
            }
        }

        private fun state(request: Request): RequestState {
            return requireNotNull(requestStates[request]) {
                "Request '$request' was not registered with the tracker"
            }
        }

        private fun isInProgress(request: Request): Boolean {
            val requestState = state(request)
            return requestState.started.count == 0L && requestState.finished.count != 0L
        }

        private fun unfinishedRequests(): List<String> {
            return requestStates.mapNotNull { (request, requestState) ->
                when {
                    requestState.finished.count == 0L -> null
                    requestState.started.count == 0L -> "${request.name} (in progress)"
                    else -> "${request.name} (not started)"
                }
            }
        }

        private fun deadlineFromNow(): Long {
            return System.nanoTime() + TimeUnit.SECONDS.toNanos(REQUEST_TIMEOUT_SECONDS)
        }

        private fun awaitWithTimeout(
            latch: CountDownLatch,
            description: String,
        ) {
            awaitUntil(
                latch = latch,
                description = description,
                deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(REQUEST_TIMEOUT_SECONDS),
            )
        }

        private fun awaitUntil(
            latch: CountDownLatch,
            description: String,
            deadlineNanos: Long,
        ) {
            val remainingNanos = deadlineNanos - System.nanoTime()
            if (remainingNanos <= 0) {
                throw AssertionError("Timed out waiting for $description.")
            }

            try {
                if (!latch.await(remainingNanos, TimeUnit.NANOSECONDS)) {
                    throw AssertionError("Timed out waiting for $description.")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw AssertionError("Interrupted while waiting for $description.", e)
            }
        }
    }

    private data class RequestState(
        val started: CountDownLatch = CountDownLatch(1),
        val releaseResponse: CountDownLatch = CountDownLatch(1),
        val finished: CountDownLatch = CountDownLatch(1),
    )
}
