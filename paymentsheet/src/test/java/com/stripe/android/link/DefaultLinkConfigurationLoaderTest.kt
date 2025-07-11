package com.stripe.android.link

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.exceptions.LinkUnavailableException
import com.stripe.android.link.gate.FakeLinkGate
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.testing.FakeLogger
import com.stripe.android.utils.FakePaymentElementLoader
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultLinkConfigurationLoaderTest {
    private val logger = FakeLogger()
    private val linkGate = FakeLinkGate()
    private val linkGateFactory = LinkGate.Factory { linkGate }
    private val configuration = LinkController.Configuration.Builder("Test Merchant").build()
    private val linkConfiguration = TestFactory.LINK_CONFIGURATION

    private val linkState = LinkState(
        configuration = linkConfiguration,
        loginState = LinkState.LoginState.LoggedIn,
        signupMode = null,
    )

    private fun createLoader(
        paymentElementLoader: FakePaymentElementLoader,
        useNativeLink: Boolean? = null
    ): DefaultLinkConfigurationLoader {
        useNativeLink?.let { linkGate.setUseNativeLink(it) }
        return DefaultLinkConfigurationLoader(
            logger = logger,
            paymentElementLoader = paymentElementLoader,
            linkGateFactory = linkGateFactory,
        )
    }

    @Test
    fun `load() returns success when LinkConfiguration is available and useNativeLink is true`() = runTest {
        val loader = createLoader(
            paymentElementLoader = FakePaymentElementLoader(linkState = linkState),
            useNativeLink = true
        )

        val result = loader.load(configuration)
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(linkConfiguration)
    }

    @Test
    fun `load() returns failure when linkState configuration is null`() = runTest {
        val loader = createLoader(
            paymentElementLoader = FakePaymentElementLoader(linkState = null)
        )

        val result = loader.load(configuration)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(LinkUnavailableException::class.java)
    }

    @Test
    fun `load() returns failure when useNativeLink is false`() = runTest {
        val loader = createLoader(
            paymentElementLoader = FakePaymentElementLoader(linkState = linkState),
            useNativeLink = false
        )

        val result = loader.load(configuration)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(LinkUnavailableException::class.java)
    }

    @Test
    fun `load() returns failure when paymentElementLoader returns failure`() = runTest {
        val loader = createLoader(
            paymentElementLoader = FakePaymentElementLoader(shouldFail = true)
        )

        val result = loader.load(configuration)
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `load() calls paymentElementLoader with expected parameters`() = runTest {
        val loader = createLoader(
            paymentElementLoader = FakePaymentElementLoader(linkState = linkState),
            useNativeLink = true
        )

        val result = loader.load(configuration)
        assertThat(result.isSuccess).isTrue()
    }
}
