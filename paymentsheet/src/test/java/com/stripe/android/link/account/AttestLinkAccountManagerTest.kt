package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

internal class AttestLinkAccountManagerTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `lookupConsumer does not start session when startSession is false`() = runSuspendTest {
        val linkRepository = object : FakeLinkRepository() {
            var callCount = 0
            override suspend fun startVerification(
                consumerSessionClientSecret: String,
                consumerPublishableKey: String?
            ): Result<ConsumerSession> {
                callCount += 1
                return super.startVerification(consumerSessionClientSecret, consumerPublishableKey)
            }
        }
        val accountManager = accountManager(linkRepository = linkRepository)

        accountManager.lookupConsumer(TestFactory.EMAIL, false)

        assertThat(linkRepository.callCount).isEqualTo(0)
        assertThat(accountManager.linkAccount.value).isNull()
    }

    private fun accountManager(
        customerEmail: String? = null,
        stripeIntent: StripeIntent = PaymentIntentFactory.create(),
        passthroughModeEnabled: Boolean = false,
        linkRepository: LinkRepository = FakeLinkRepository(),
        linkEventsReporter: LinkEventsReporter = AccountManagerEventsReporter()
    ): AttestLinkAccountManager {
        val customerInfo = TestFactory.LINK_CONFIGURATION.customerInfo.copy(
            email = customerEmail,
        )
        val config = TestFactory.LINK_CONFIGURATION.copy(
            stripeIntent = stripeIntent,
            passthroughModeEnabled = passthroughModeEnabled,
            customerInfo = customerInfo
        )
        val linkAccountManager = DefaultLinkAccountManager(
            config = config,
            linkRepository,
            linkEventsReporter,
            errorReporter = FakeErrorReporter()
        )
        return AttestLinkAccountManager(
            config = config,
            defaultLinkAccountManager = linkAccountManager,
            integrityRequestManager = mock(),
            linkRepository = linkRepository,
            applicationId = TestFactory.APP_ID
        )
    }

    private fun runSuspendTest(testBody: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        testBody()
    }
}