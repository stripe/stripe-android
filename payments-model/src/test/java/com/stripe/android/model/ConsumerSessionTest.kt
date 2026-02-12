package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerSession.AuthenticationLevel
import org.junit.Test

class ConsumerSessionTest {

    @Test
    fun `meetsMinimumAuthenticationLevel returns true when current equals minimum`() {
        val session = makeSession(
            current = AuthenticationLevel.OneFactorAuthentication,
            minimum = AuthenticationLevel.OneFactorAuthentication,
        )
        assertThat(session.meetsMinimumAuthenticationLevel).isTrue()
    }

    @Test
    fun `meetsMinimumAuthenticationLevel returns false when current is below minimum`() {
        val session = makeSession(
            current = AuthenticationLevel.NotAuthenticated,
            minimum = AuthenticationLevel.OneFactorAuthentication,
        )
        assertThat(session.meetsMinimumAuthenticationLevel).isFalse()
    }

    @Test
    fun `meetsMinimumAuthenticationLevel returns true when current exceeds minimum`() {
        val session = makeSession(
            current = AuthenticationLevel.TwoFactorAuthentication,
            minimum = AuthenticationLevel.OneFactorAuthentication,
        )
        assertThat(session.meetsMinimumAuthenticationLevel).isTrue()
    }

    @Test
    fun `meetsMinimumAuthenticationLevel returns false when current is null`() {
        val session = makeSession(
            current = null,
            minimum = AuthenticationLevel.OneFactorAuthentication,
        )
        assertThat(session.meetsMinimumAuthenticationLevel).isFalse()
    }

    @Test
    fun `meetsMinimumAuthenticationLevel returns false when minimum is null`() {
        val session = makeSession(
            current = AuthenticationLevel.OneFactorAuthentication,
            minimum = null,
        )
        assertThat(session.meetsMinimumAuthenticationLevel).isFalse()
    }

    @Test
    fun `AuthenticationLevel ordering is Unknown less than NotAuthenticated less than 1FA less than 2FA`() {
        assertThat(AuthenticationLevel.Unknown < AuthenticationLevel.NotAuthenticated).isTrue()
        assertThat(AuthenticationLevel.NotAuthenticated < AuthenticationLevel.OneFactorAuthentication).isTrue()
        assertThat(AuthenticationLevel.OneFactorAuthentication < AuthenticationLevel.TwoFactorAuthentication).isTrue()
    }

    private fun makeSession(
        current: AuthenticationLevel?,
        minimum: AuthenticationLevel?,
    ): ConsumerSession {
        return ConsumerSession(
            clientSecret = "secret",
            emailAddress = "test@stripe.com",
            redactedPhoneNumber = "+1********56",
            redactedFormattedPhoneNumber = "(***) *** **56",
            currentAuthenticationLevel = current,
            minimumAuthenticationLevel = minimum,
        )
    }
}
