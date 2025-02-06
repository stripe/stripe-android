package com.stripe.android.link.express

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.TestFactory
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.DummyActivityResultCaller
import com.stripe.android.utils.FakeActivityResultRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class LinkExpressLauncherTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `register call should register ActivityResultLauncher with ActivityResultRegistry`() {
        val expectedResult = LinkExpressResult.Canceled(LinkAccountUpdate.None)
        val linkExpressContract = LinkExpressContract()
        val linkExpressLauncher = LinkExpressLauncher(linkExpressContract)
        val activityResultRegistry = FakeActivityResultRegistry(expectedResult)
        var result: LinkExpressResult? = null

        linkExpressLauncher.register(activityResultRegistry) {
            result = it
        }

        linkExpressLauncher.present(
            configuration = TestFactory.LINK_CONFIGURATION,
            linkAccount = TestFactory.LINK_ACCOUNT
        )

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `register call should register ActivityResultLauncher with ActivityResultCaller`() = runTest {
        DummyActivityResultCaller.test {
            val linkExpressContract = LinkExpressContract()
            val linkExpressLauncher = LinkExpressLauncher(linkExpressContract)

            linkExpressLauncher.register(activityResultCaller) {}

            linkExpressLauncher.present(
                configuration = TestFactory.LINK_CONFIGURATION,
                linkAccount = TestFactory.LINK_ACCOUNT
            )

            val registerCall = awaitRegisterCall()
            assertThat(registerCall).isNotNull()

            val launchCall = awaitLaunchCall() as? LinkExpressContract.Args

            assertThat(launchCall?.linkAccount).isEqualTo(TestFactory.LINK_ACCOUNT)
            assertThat(launchCall?.configuration).isEqualTo(TestFactory.LINK_CONFIGURATION)

            awaitNextRegisteredLauncher()
        }
    }

    @Test
    fun `unregister call should unregister ActivityResultLauncher`() = runTest {
        DummyActivityResultCaller.test {
            val linkExpressContract = LinkExpressContract()
            val linkExpressLauncher = LinkExpressLauncher(linkExpressContract)

            linkExpressLauncher.register(activityResultCaller) {}

            awaitRegisterCall()
            awaitNextRegisteredLauncher()

            linkExpressLauncher.unregister()

            awaitNextUnregisteredLauncher()
        }
    }
}
