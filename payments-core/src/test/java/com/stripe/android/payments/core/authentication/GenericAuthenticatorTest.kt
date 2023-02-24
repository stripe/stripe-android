package com.stripe.android.payments.core.authentication

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenericAuthenticatorTest {

    private val lifecycleOwner = TestLifecycleOwner()

    private val host = mock<AuthActivityStarterHost> {
        on { lifecycleOwner } doReturn lifecycleOwner
    }

    @Test
    fun `Only starts authentication flow once lifecycle changes to resumed`() = runTest {
        val authenticator = TestAuthenticator()
        lifecycleOwner.currentState = Lifecycle.State.CREATED

        authenticator.authenticate(
            host = host,
            authenticatable = mock(),
            requestOptions = mock(),
        )

        assertThat(authenticator.wasInvoked).isFalse()

        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        assertThat(authenticator.wasInvoked).isTrue()
    }

    @Test
    fun `Immediately starts authentication flow if lifecycle already resumed`() = runTest {
        val authenticator = TestAuthenticator()
        lifecycleOwner.currentState = Lifecycle.State.RESUMED

        authenticator.authenticate(
            host = host,
            authenticatable = mock(),
            requestOptions = mock(),
        )

        assertThat(authenticator.wasInvoked).isTrue()
    }
}

private class TestAuthenticator : PaymentAuthenticator<StripeIntent>() {

    var wasInvoked: Boolean = false
        private set

    override suspend fun performAuthentication(
        host: AuthActivityStarterHost,
        authenticatable: StripeIntent,
        requestOptions: ApiRequest.Options
    ) {
        wasInvoked = true
    }
}
