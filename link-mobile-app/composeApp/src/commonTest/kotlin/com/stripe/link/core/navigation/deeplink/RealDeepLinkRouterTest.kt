package com.stripe.link.core.navigation.deeplink

import com.stripe.link.core.data.network.ApiRequestFactory
import com.stripe.link.core.data.network.LinkApiClient
import com.stripe.link.core.data.storage.LocalFeatureFlag
import com.stripe.link.core.data.storage.LocalFeatureFlagsStore
import com.stripe.link.feature.common.LinkDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RealDeepLinkRouterTest {

    private lateinit var fakeLinkApiClient: FakeLinkApiClient

    @BeforeTest
    fun setUp() {
        fakeLinkApiClient = FakeLinkApiClient()
        LocalFeatureFlagsStore.reset()
    }

    @AfterTest
    fun tearDown() {
        LocalFeatureFlagsStore.reset()
    }

    // 1. Flag disabled → recordEmailClick is NOT called, routing still works
    @Test
    fun `flag disabled - recordEmailClick not called, routing still works`() = runScenario(
        emailClickTrackingEnabled = false,
    ) {
        router.parseDeepLink(
            "https://app.link.com/wallet?ref=link_monthly_transaction_summary&eid=em_abc123"
        )
        advanceUntilIdle()
        fakeLinkApiClient.ensureNoEmailClickCalls()
    }

    // 2. Flag enabled, URL has no eid param → recordEmailClick NOT called
    @Test
    fun `flag enabled, no eid param - recordEmailClick not called`() = runScenario(
        emailClickTrackingEnabled = true,
    ) {
        router.parseDeepLink("https://app.link.com/wallet?ref=link_monthly_transaction_summary")
        advanceUntilIdle()
        fakeLinkApiClient.ensureNoEmailClickCalls()
    }

    // 3. Flag enabled, URL has no ref param → recordEmailClick NOT called
    @Test
    fun `flag enabled, no ref param - recordEmailClick not called`() = runScenario(
        emailClickTrackingEnabled = true,
    ) {
        router.parseDeepLink("https://app.link.com/wallet?eid=em_abc123")
        advanceUntilIdle()
        fakeLinkApiClient.ensureNoEmailClickCalls()
    }

    // 4. Flag enabled, URL has both ref and eid → recordEmailClick called with correct args
    @Test
    fun `flag enabled, ref and eid present - recordEmailClick called with correct args`() = runScenario(
        emailClickTrackingEnabled = true,
    ) {
        val url = "https://app.link.com/wallet?ref=link_monthly_transaction_summary&eid=em_abc123"
        router.parseDeepLink(url)
        advanceUntilIdle()

        val call = fakeLinkApiClient.recordEmailClickCalls.removeFirst()
        assertEquals("link_monthly_transaction_summary", call.ref)
        assertEquals("em_abc123", call.eid)
        assertEquals(url, call.link)
        fakeLinkApiClient.ensureNoEmailClickCalls()
    }

    // 5. recordEmailClick throws → exception swallowed, parseDeepLink still returns correctly
    @Test
    fun `recordEmailClick throws - exception swallowed, parseDeepLink completes normally`() = runScenario(
        emailClickTrackingEnabled = true,
    ) {
        fakeLinkApiClient.shouldThrow = true
        val url = "https://app.link.com/wallet?ref=link_monthly_transaction_summary&eid=em_abc123"

        // Should not throw; fire-and-forget swallows errors
        val result = router.parseDeepLink(url)
        advanceUntilIdle()

        // parseDeepLink completes normally regardless of the network error
        assertTrue(fakeLinkApiClient.threwOnCall)
    }

    // 6. Normal deep link with no email params → routing works, no network call regardless of flag
    @Test
    fun `normal deep link with no email params - no network call regardless of flag state`() = runScenario(
        emailClickTrackingEnabled = true,
    ) {
        router.parseDeepLink("https://app.link.com/wallet")
        advanceUntilIdle()
        fakeLinkApiClient.ensureNoEmailClickCalls()
    }

    // ── Scenario setup ────────────────────────────────────────────────────────────

    private lateinit var router: RealDeepLinkRouter

    private fun runScenario(
        emailClickTrackingEnabled: Boolean = false,
        block: suspend TestScope.() -> Unit,
    ) = runTest {
        LocalFeatureFlagsStore.setValue(LocalFeatureFlag.EmailClickTracking, emailClickTrackingEnabled)

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = LinkDispatchers(
            Main = testDispatcher,
            IO = testDispatcher,
        )

        router = RealDeepLinkRouter(
            linkApiClient = fakeLinkApiClient,
            dispatchers = dispatchers,
        )

        block()
    }
}

// ── Fakes ──────────────────────────────────────────────────────────────────────

internal class FakeLinkApiClient : LinkApiClient(
    apiRequestFactory = ApiRequestFactory(requestSurface = "test"),
) {
    data class EmailClickCall(val ref: String, val eid: String, val link: String)

    val recordEmailClickCalls = mutableListOf<EmailClickCall>()
    var shouldThrow = false
    var threwOnCall = false

    override suspend fun recordEmailClick(ref: String, eid: String, link: String) {
        if (shouldThrow) {
            threwOnCall = true
            throw RuntimeException("Simulated recordEmailClick failure")
        }
        recordEmailClickCalls.add(EmailClickCall(ref = ref, eid = eid, link = link))
    }

    fun ensureNoEmailClickCalls() {
        assertTrue(
            recordEmailClickCalls.isEmpty(),
            "Expected no recordEmailClick calls but got: $recordEmailClickCalls"
        )
    }
}
