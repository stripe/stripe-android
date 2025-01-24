package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.link.FakeIntegrityRequestManager
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.TestFactory
import com.stripe.android.link.repositories.FakeLinkRepository
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.attestation.IntegrityRequestManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

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
    fun `lookupConsumer returns link account on success`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val (attestAccountManager, _) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        val lookupResult = attestAccountManager.lookupConsumer(TestFactory.EMAIL, startSession = false)

        integrityRequestManager.awaitRequestTokenCall()
        val lookupCall = linkRepository.awaitMobileLookup()
        assertThat(lookupCall.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(lookupCall.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(lookupResult.getOrNull()?.email)
            .isEqualTo(TestFactory.CONSUMER_SESSION_LOOKUP.consumerSession?.emailAddress)
        integrityRequestManager.ensureAllEventsConsumed()
        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `lookupConsumer returns error on lookup failure`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = FakeLinkRepository()
        val integrityRequestManager = FakeIntegrityRequestManager()

        linkRepository.mobileLookupConsumerResult = Result.failure(error)

        val (attestAccountManager, _) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        val lookupResult = attestAccountManager.lookupConsumer(TestFactory.EMAIL, startSession = false)

        integrityRequestManager.awaitRequestTokenCall()
        linkRepository.awaitMobileLookup()
        assertThat(lookupResult.exceptionOrNull()).isEqualTo(error)
        integrityRequestManager.ensureAllEventsConsumed()
        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `lookupConsumer returns error on attestation failure`() = runSuspendTest {
        val error = Throwable("oops")
        val integrityRequestManager = FakeIntegrityRequestManager()
        val linkRepository = FakeLinkRepository()
        integrityRequestManager.requestResult = Result.failure(error)

        val (attestAccountManager, _) = accountManager(
            integrityRequestManager = integrityRequestManager,
            linkRepository = linkRepository
        )

        val lookupResult = attestAccountManager.lookupConsumer(TestFactory.EMAIL, startSession = false)

        integrityRequestManager.awaitRequestTokenCall()
        assertThat(lookupResult.exceptionOrNull()).isEqualTo(error)
        integrityRequestManager.ensureAllEventsConsumed()
        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `lookupConsumer does not start session when startSession is false`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        val (attestAccountManager, defaultAccountManager) = accountManager(linkRepository = linkRepository)

        attestAccountManager.lookupConsumer(TestFactory.EMAIL, startSession = false)

        defaultAccountManager.ensureAllEventsConsumed()
    }

    @Test
    fun `lookupConsumer starts session when startSession is true`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        linkRepository.lookupConsumerResult = Result.success(TestFactory.CONSUMER_SESSION_LOOKUP)
        val (attestAccountManager, defaultAccountManager) = accountManager(linkRepository = linkRepository)

        attestAccountManager.lookupConsumer(TestFactory.EMAIL, startSession = true)

        val call = defaultAccountManager.awaitSetAccountCall()
        assertThat(call.consumerSession).isEqualTo(TestFactory.CONSUMER_SESSION_LOOKUP.consumerSession)
        assertThat(call.publishableKey).isEqualTo(TestFactory.CONSUMER_SESSION_LOOKUP.publishableKey)
    }

    @Test
    fun `signUp returns link account on success`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()
        val integrityRequestManager = FakeIntegrityRequestManager()
        val (attestAccountManager, _) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        val signupResult = attestAccountManager.signUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()
        val signupCall = linkRepository.awaitMobileSignup()
        assertThat(signupCall.verificationToken).isEqualTo(TestFactory.VERIFICATION_TOKEN)
        assertThat(signupCall.appId).isEqualTo(TestFactory.APP_ID)
        assertThat(signupResult.getOrNull()?.email)
            .isEqualTo(TestFactory.CONSUMER_SESSION_LOOKUP.consumerSession?.emailAddress)
        integrityRequestManager.ensureAllEventsConsumed()
        linkRepository.ensureAllEventsConsumed()
    }

    @Test
    fun `signUp returns error on repository failure`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = FakeLinkRepository()
        linkRepository.mobileConsumerSignUpResult = Result.failure(error)

        val integrityRequestManager = FakeIntegrityRequestManager()

        val (attestAccountManager, _) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        val signupResult = attestAccountManager.signUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()
        linkRepository.awaitMobileSignup()
        assertThat(signupResult.exceptionOrNull()).isEqualTo(error)
        linkRepository.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `signUp returns error on setAccount failure`() = runSuspendTest {
        val linkRepository = FakeLinkRepository()

        val integrityRequestManager = FakeIntegrityRequestManager()

        val (attestAccountManager, defaultLinkAccountManager) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        defaultLinkAccountManager.setAccountResult = null

        val signupResult = attestAccountManager.signUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()
        linkRepository.awaitMobileSignup()
        assertThat(signupResult.exceptionOrNull()).isInstanceOf<NoLinkAccountFoundException>()
        linkRepository.ensureAllEventsConsumed()
        integrityRequestManager.ensureAllEventsConsumed()
    }

    @Test
    fun `signUp returns error on attestation failure`() = runSuspendTest {
        val error = Throwable("oops")
        val linkRepository = FakeLinkRepository()

        val integrityRequestManager = FakeIntegrityRequestManager()

        integrityRequestManager.requestResult = Result.failure(error)

        val (attestAccountManager, _) = accountManager(
            linkRepository = linkRepository,
            integrityRequestManager = integrityRequestManager
        )

        val signupResult = attestAccountManager.signUp(
            email = TestFactory.EMAIL,
            phone = TestFactory.CUSTOMER_PHONE,
            country = TestFactory.COUNTRY,
            name = TestFactory.CUSTOMER_NAME,
            consentAction = SignUpConsentAction.Implied
        )

        integrityRequestManager.awaitRequestTokenCall()
        assertThat(signupResult.exceptionOrNull()).isEqualTo(error)
        integrityRequestManager.ensureAllEventsConsumed()
        linkRepository.ensureAllEventsConsumed()
    }

    private fun accountManager(
        linkRepository: LinkRepository = FakeLinkRepository(),
        integrityRequestManager: IntegrityRequestManager = FakeIntegrityRequestManager()
    ): Pair<AttestLinkAccountManager, FakeLinkAccountManager> {
        val defaultLinkAccountManager = FakeLinkAccountManager()
        val attestLinkAccountManager = AttestLinkAccountManager(
            config = TestFactory.LINK_CONFIGURATION,
            defaultLinkAccountManager = defaultLinkAccountManager,
            integrityRequestManager = integrityRequestManager,
            linkRepository = linkRepository,
            applicationId = TestFactory.APP_ID
        )
        return attestLinkAccountManager to defaultLinkAccountManager
    }

    private fun runSuspendTest(testBody: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        testBody()
    }
}
