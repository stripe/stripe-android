package com.stripe.android.link.account

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.TestFactory
import com.stripe.android.link.model.AccountStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AutoLoginLinkAccountManagerTest {
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
    fun `When customerEmail is set in arguments then it is looked up`() = runTest(dispatcher) {
        val defaultLinkAccountManager = FakeLinkAccountManager()
        defaultLinkAccountManager.lookupConsumerResult = Result.success(TestFactory.LINK_ACCOUNT)

        val accountManager = accountManager(
            email = TestFactory.EMAIL,
            defaultLinkAccountManager = defaultLinkAccountManager
        )

        assertThat(
            accountManager.accountStatus.first()
        ).isEqualTo(AccountStatus.Verified)
    }

    @Test
    fun `When customerEmail is set and look up fails, then account status is Error`() = runTest(dispatcher) {
        val error = Throwable("oops")
        val defaultLinkAccountManager = FakeLinkAccountManager()
        defaultLinkAccountManager.lookupConsumerResult = Result.failure(error)

        val accountManager = accountManager(
            email = TestFactory.EMAIL,
            defaultLinkAccountManager = defaultLinkAccountManager
        )

        assertThat(
            accountManager.accountStatus.first()
        ).isEqualTo(AccountStatus.Error)
    }

    @Test
    fun `When customerEmail is not set, then account status in SignedOut`() = runTest(dispatcher) {
        val defaultLinkAccountManager = FakeLinkAccountManager()

        val accountManager = accountManager(
            defaultLinkAccountManager = defaultLinkAccountManager
        )

        assertThat(
            accountManager.accountStatus.first()
        ).isEqualTo(AccountStatus.SignedOut)
    }

    @Test
    fun `lookup happens only once`() = runTest(dispatcher) {
        val defaultLinkAccountManager = FakeLinkAccountManager()

        val accountManager = accountManager(
            defaultLinkAccountManager = defaultLinkAccountManager
        )

        assertThat(
            accountManager.accountStatus.first()
        ).isEqualTo(AccountStatus.SignedOut)
    }

    private fun accountManager(
        email: String? = null,
        defaultLinkAccountManager: FakeLinkAccountManager
    ): AutoLoginLinkAccountManager {
        return AutoLoginLinkAccountManager(
            configuration = TestFactory.LINK_CONFIGURATION.copy(
                customerInfo = TestFactory.LINK_CUSTOMER_INFO.copy(
                    email = email
                )
            ),
            defaultLinkAccountManager = defaultLinkAccountManager
        )
    }
}